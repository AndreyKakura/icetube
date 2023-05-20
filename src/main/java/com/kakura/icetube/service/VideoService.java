package com.kakura.icetube.service;

import com.kakura.icetube.dto.*;
import com.kakura.icetube.exception.BadRequestException;
import com.kakura.icetube.exception.NotFoundException;
import com.kakura.icetube.mapper.CommentMapper;
import com.kakura.icetube.mapper.VideoMapper;
import com.kakura.icetube.model.*;
import com.kakura.icetube.repository.CommentRepository;
import com.kakura.icetube.repository.SubscriptionRepository;
import com.kakura.icetube.repository.TagRepository;
import com.kakura.icetube.repository.VideoRepository;
import lombok.extern.log4j.Log4j2;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.kakura.icetube.util.VideoUtil.removeFileExt;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

@Service
@Log4j2
public class VideoService {

    @Value("${data.folder}")
    private String dataFolder;

    @Value("${pass.ffmpeg}")
    private String ffmpegPass;

    @Value("${pass.ffprobe}")
    private String ffprobePass;

    private final VideoRepository videoRepository;

    private final TagRepository tagRepository;

    private final FrameGrabberService frameGrabberService;

    private final VideoMapper videoMapper;

    private final UserService userService;

    private final CommentMapper commentMapper;

    private final CommentRepository commentRepository;

    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public VideoService(VideoRepository videoRepository, TagRepository tagRepository, FrameGrabberService frameGrabberService, VideoMapper videoMapper, UserService userService, CommentMapper commentMapper, CommentRepository commentRepository, SubscriptionRepository subscriptionRepository) {
        this.videoRepository = videoRepository;
        this.tagRepository = tagRepository;
        this.frameGrabberService = frameGrabberService;
        this.videoMapper = videoMapper;
        this.userService = userService;
        this.commentMapper = commentMapper;
        this.commentRepository = commentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }


    public VideoPageDto findPage(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Video> videos = videoRepository.findAll(pageable);
        if (videos.hasContent()) {
            return new VideoPageDto(videos.stream().map(videoMapper::toDto).collect(Collectors.toList()), videos.getTotalPages());
        } else {
            return new VideoPageDto(null, videos.getTotalPages());
        }
    }

    @Transactional
    public VideoDto findById(Long id) {
        Video videoFromDb = videoRepository.findById(id).
                orElseThrow(() -> new NotFoundException("Cannot find video by id " + id));

        videoFromDb.incrementViewCount();
        videoRepository.save(videoFromDb);
        VideoDto videoDto = videoMapper.toDto(videoFromDb);

        if (userService.isLoggedIn()) {
            userService.addToWatchedVideos(videoFromDb);
            videoDto.setIsSubscribedToAuthor(userService.isSubscribedToAuthor(videoFromDb.getUser()));
            videoDto.setIsAuthor(isAuthor(videoFromDb));
        }


        return videoDto;
    }

    private boolean isAuthor(Video video) {
        return video.getUser().equals(userService.getCurrentUser());
    }

    @Transactional
    public void saveNewVideo(NewVideoDto newVideoDto) {
        Video video = videoMapper.toModel(newVideoDto);
        if (newVideoDto.getTags() != null) {
            video.setTags(convertStringsToTags(newVideoDto.getTags()));
        }
        video.setUser(userService.getCurrentUser());

        Video savedVideo = videoRepository.save(video);

        Path directory = Path.of(dataFolder, video.getId().toString());
        try {
            Files.createDirectories(directory);
            Path videoDirectory = Path.of(directory.toString());
            Files.createDirectories(videoDirectory);

            Path videoPath = Path.of(videoDirectory.toString(), newVideoDto.getVideoFile().getOriginalFilename());
            try (OutputStream out = Files.newOutputStream(videoPath, CREATE, WRITE)) {
                newVideoDto.getVideoFile().getInputStream().transferTo(out);
            }

            Path previewPath = Path.of(directory.toString(), newVideoDto.getPreviewFile().getOriginalFilename());
            try (OutputStream out = Files.newOutputStream(previewPath, CREATE, WRITE)) {
                newVideoDto.getPreviewFile().getInputStream().transferTo(out);
            }

            long length = frameGrabberService.lengthInTime(videoPath);
            video.setVideoLength(length);

            int videoWidth = 0;
            int videoHeight = 0;
            try (FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoPath.toString())) {
                frameGrabber.start();
                videoWidth = frameGrabber.getImageWidth();
                videoHeight = frameGrabber.getImageHeight();
                frameGrabber.stop();
            } catch (Exception e) {
                log.error("Failed to get video resolution", e);
                throw new IllegalStateException();
            }

            video.setVideoResolution(videoHeight);

            if (videoWidth >= 1920 && videoHeight >= 1080) {
                String maxResolution = "1080p";
                Path maxResolutionVideoDirectory = Path.of(directory.toString(), maxResolution);
                Files.createDirectories(maxResolutionVideoDirectory);
                Path maxResolutionVideoPath = Path.of(maxResolutionVideoDirectory.toString(), newVideoDto.getVideoFile().getOriginalFilename());
                createLowerResolutionVideo(videoPath, maxResolutionVideoPath, 1920, 1080, 3_000_000);
            }

            if (videoWidth >= 1280 && videoHeight >= 720) {
                String lowerResolution = "720p";
                Path lowerVideoDirectory = Path.of(directory.toString(), lowerResolution);
                Files.createDirectories(lowerVideoDirectory);
                Path lowerVideoPath = Path.of(lowerVideoDirectory.toString(), newVideoDto.getVideoFile().getOriginalFilename());
                createLowerResolutionVideo(videoPath, lowerVideoPath, 1280, 720, 2_000_000);
            }

            if (videoWidth >= 720 && videoHeight >= 480) {
                String lowerResolution = "480p";
                Path lowerVideoDirectory = Path.of(directory.toString(), lowerResolution);
                Files.createDirectories(lowerVideoDirectory);
                Path lowerVideoPath = Path.of(lowerVideoDirectory.toString(), newVideoDto.getVideoFile().getOriginalFilename());
                createLowerResolutionVideo(videoPath, lowerVideoPath, 720, 480, 1_200_000);
            }

            if (videoWidth >= 640 && videoHeight >= 360) {
                String lowerResolution = "360p";
                Path lowerVideoDirectory = Path.of(directory.toString(), lowerResolution);
                Files.createDirectories(lowerVideoDirectory);
                Path lowerVideoPath = Path.of(lowerVideoDirectory.toString(), newVideoDto.getVideoFile().getOriginalFilename());
                createLowerResolutionVideo(videoPath, lowerVideoPath, 640, 360, 700_000);
            }

            Files.delete(videoPath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createLowerResolutionVideo(Path sourceVideoPath, Path outputVideoPath, int targetWidth, int targetHeight, long bitrate) throws IOException {
        FFmpeg ffmpeg = new FFmpeg(ffmpegPass);
        FFprobe ffprobe = new FFprobe(ffprobePass);

        FFmpegBuilder builder = new FFmpegBuilder()

                .setInput(sourceVideoPath.toString())     // Filename, or a FFmpegProbeResult
                .overrideOutputFiles(true) // Override the output if it exists

                .addOutput(outputVideoPath.toString())   // Filename for the destination
                .setFormat("mp4")        // Format is inferred from filename, or can be set
//                .setTargetSize(250_000)  // Aim for a 250KB file
                .setVideoBitRate(bitrate)
                .disableSubtitle()       // No subtiles

                .setAudioChannels(1)         // Mono audio
                .setAudioCodec("aac")        // using the aac codec
                .setAudioSampleRate(48_000)  // at 48KHz
                .setAudioBitRate(32768)      // at 32 kbit/s

                .setVideoCodec("libx264")     // Video using x264
                .setVideoFrameRate(24, 1)     // at 24 frames per second
                .setVideoResolution(targetWidth, targetHeight) // at 640x480 resolution

                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

// Run a one-pass encode
        executor.createJob(builder).run();
// Or run a two-pass encode (which is better quality at the cost of being slower)
//        executor.createTwoPassJob(builder).run();
    }

    private String getVideoResolution(MultipartFile videoFile) throws IOException {
        try (FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoFile.getInputStream())) {
            frameGrabber.start();
            int videoWidth = frameGrabber.getImageWidth();
            int videoHeight = frameGrabber.getImageHeight();
            frameGrabber.stop();

            if (videoWidth >= 1920 && videoHeight >= 1080) {
                return "1080p";
            } else if (videoWidth >= 1280 && videoHeight >= 720) {
                return "720p";
            } else if (videoWidth >= 720 && videoHeight >= 480) {
                return "480p";
            } else {
                return "360p";
            }
        } catch (Exception e) {
            log.error("Failed to get video resolution", e);
            throw new IllegalStateException();
        }
    }


    public byte[] getPreviewBytes(Long id) {
        Video videoFromDb = videoRepository.findById(id).orElseThrow(() -> new NotFoundException("Cannot find preview by id" + id));
        byte[] imageBytes = new byte[0];
        try {
            imageBytes = Files.readAllBytes(Paths.get(dataFolder, videoFromDb.getId().toString(), removeFileExt(videoFromDb.getPreviewFileName()) + videoFromDb.getPreviewContentType()));
        } catch (IOException e) {
            log.error(e);
            throw new NotFoundException("Cannot get preview by id " + id);
        }
        return imageBytes;
    }

    public Optional<StreamBytesInfo> getStreamBytes(Long id, HttpRange range, String quality) {
        Optional<Video> byId = videoRepository.findByIdWithCache(id);
        if (byId.isEmpty()) {
            return Optional.empty();
        }
        Path filePath = Path.of(dataFolder, Long.toString(id), quality, byId.get().getVideoFileName());
        if (!Files.exists(filePath)) {
            log.error("File {} not found", filePath);
            return Optional.empty();
        }
        try {
            long fileSize = Files.size(filePath);
            long chunkSize = fileSize / 50;
            if (range == null) {
                return Optional.of(new StreamBytesInfo(out -> Files.newInputStream(filePath).transferTo(out), fileSize, 0, fileSize, byId.get().getVideoContentType()));
            }

            long rangeStart = range.getRangeStart(0);
            long rangeEnd = rangeStart + chunkSize; // range.getRangeEnd(fileSize);
            if (rangeEnd >= fileSize) {
                rangeEnd = fileSize - 1;
            }
            long finalRangeEnd = rangeEnd;
            return Optional.of(new StreamBytesInfo(out -> {
                try (InputStream inputStream = Files.newInputStream(filePath)) {
                    inputStream.skip(rangeStart);
                    byte[] bytes = inputStream.readNBytes((int) ((finalRangeEnd - rangeStart) + 1));
                    out.write(bytes);
                }
            }, fileSize, rangeStart, rangeEnd, byId.get().getVideoContentType()));
        } catch (IOException ex) {
            log.error("", ex);
            return Optional.empty();
        }
    }


    public EditVideoDto editVideo(EditVideoDto editVideoDto) {
        Video videoFromDb = videoRepository.findById(editVideoDto.getId()).orElseThrow(() -> new NotFoundException("Cannot find video by id - " + editVideoDto.getId()));

        videoFromDb.setTitle(editVideoDto.getTitle());
        videoFromDb.setDescription(editVideoDto.getDescription());
        videoFromDb.setTags(convertStringsToTags(editVideoDto.getTags()));
//        videoFromDb.setPreviewUrl(editVideoDto.getPreviewUrl()); //TODO
        //setVideoUrl //TODO
        videoRepository.save(videoFromDb);
        return editVideoDto;
    }


    private Set<Tag> convertStringsToTags(Set<String> stringTags) {

        Set<Tag> tags = new HashSet<>();

        for (String tagText : stringTags) {
            Optional<Tag> optionalTag = tagRepository.findByTagText(tagText.toUpperCase());
            if (optionalTag.isPresent()) {
                tags.add(optionalTag.get());
            } else {
                Tag tag = tagRepository.save(new Tag(tagText.toUpperCase()));
                tags.add(tag);
            }
        }
        return tags;
    }

    public void uploadPreview(MultipartFile file, Long videoId) {
        Video videoFromDb = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Cannot find video by id - " + videoId));

        Path directory = Path.of(dataFolder, videoId.toString());
        try {
            Path path = Path.of(directory.toString(), file.getOriginalFilename());
            try (OutputStream out = Files.newOutputStream(path, CREATE, WRITE)) {
                file.getInputStream().transferTo(out);
            }
        } catch (IOException e) {
            log.error("", e);
            throw new IllegalStateException();
        }
    }

    public VideoDto likeVideo(Long id) {
        Video videoFromDb = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Cannot find video by id - " + id));

        if (userService.ifLikedVideo(id)) {
            videoFromDb.decrementLikes();
            userService.removeFromLikedVideos(videoFromDb);
        } else if (userService.ifDislikedVideo(id)) {
            videoFromDb.decrementDislikes();
            userService.removeFromDislikedVideos(videoFromDb);
            videoFromDb.incrementLikes();
            userService.addToLikedVideos(videoFromDb);
        } else {
            videoFromDb.incrementLikes();
            userService.addToLikedVideos(videoFromDb);
        }

        videoRepository.save(videoFromDb);

        return videoMapper.toDto(videoFromDb);

    }

    public VideoDto dislikeVideo(Long id) {
        Video videoFromDb = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Cannot find video by id - " + id));

        if (userService.ifDislikedVideo(id)) {
            videoFromDb.decrementDislikes();
            userService.removeFromDislikedVideos(videoFromDb);
        } else if (userService.ifLikedVideo(id)) {
            videoFromDb.decrementLikes();
            userService.removeFromLikedVideos(videoFromDb);
            videoFromDb.incrementDislikes();
            userService.addToDisLikedVideos(videoFromDb);
        } else {
            videoFromDb.incrementDislikes();
            userService.addToDisLikedVideos(videoFromDb);
        }

        videoRepository.save(videoFromDb);

        return videoMapper.toDto(videoFromDb);

    }

    public void addComment(Long id, CommentDto commentDto) {
        Video videoFromDb = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Cannot find video by id - " + id));

        Comment comment = commentMapper.toModel(commentDto);

        User user = userService.getCurrentUser();

        comment.setUser(user);
        comment.setVideo(videoFromDb);

        commentRepository.save(comment);

        videoFromDb.addComment(comment);

        videoRepository.save(videoFromDb);
    }

    public List<CommentDto> getAllComments(Long id) {
        Video videoFromDb = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Cannot find video by id - " + id));

        List<Comment> comments = videoFromDb.getComments();

        return comments.stream().map(commentMapper::toDto).toList();
    }

    public VideoPageDto getVideoHistoryPage(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        User currentUser = userService.getCurrentUser();
        Page<Video> videos = videoRepository.findByUsersWhoWatchedIn(List.of(currentUser), pageable);
        if (videos.hasContent()) {
            return new VideoPageDto(videos.stream().map(videoMapper::toDto).collect(Collectors.toList()), videos.getTotalPages());
        } else {
            return new VideoPageDto(null, videos.getTotalPages());
        }
    }

    public VideoPageDto findPublishedByUserIdPage(Long userId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Video> videos = videoRepository.findAllByUserId(userId, pageable);
        if (videos.hasContent()) {
            return new VideoPageDto(videos.stream().map(videoMapper::toDto).collect(Collectors.toList()), videos.getTotalPages());
        } else {
            return new VideoPageDto(null, videos.getTotalPages());
        }
    }

    public VideoPageDto getSubscribedVideosPage(int pageNumber, int pageSize) {
        User currentUser = userService.getCurrentUser();
        List<User> userSubscriptions = subscriptionRepository.findBySubscriber(currentUser).stream()
                .map(Subscription::getSubscribedTo).toList();
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Video> videos = videoRepository.findByUserIn(userSubscriptions, pageable);
        if (videos.hasContent()) {
            return new VideoPageDto(videos.stream().map(videoMapper::toDto).collect(Collectors.toList()), videos.getTotalPages());
        } else {
            return new VideoPageDto(null, videos.getTotalPages());
        }
    }

    public VideoPageDto getLikedVideos(int pageNumber, int pageSize) {
        User currentUser = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Video> videos = videoRepository.findByUsersWhoLikedIn(List.of(currentUser), pageable);
        if (videos.hasContent()) {
            return new VideoPageDto(videos.stream().map(videoMapper::toDto).collect(Collectors.toList()), videos.getTotalPages());
        } else {
            return new VideoPageDto(null, videos.getTotalPages());
        }
    }

    public ResponseEntity<Resource> downloadVideo(Long id, String quality) {

        Video videoById = videoRepository.findById(id).
                orElseThrow(() -> new NotFoundException("Cannot find video by id " + id));

        Path filePath = Path.of(dataFolder, id.toString(), quality, videoById.getVideoFileName());

        File file = new File(filePath.toString());
        InputStreamResource resource;
        try {
            resource = new InputStreamResource(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder
                .encode(videoById.getVideoFileName(), StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @Transactional
    public void deleteVideo(Long id) {
        User currentUser = userService.getCurrentUser();
        Video videoById = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cannot find video by id " + id));

        if (!videoById.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Cannot delete video posted by other user");
        }

        videoRepository.deleteById(id);

        for (User user : videoById.getUsersWhoLiked()) {
            user.getLikedVideos().remove(videoById);
        }

        for (User user : videoById.getUsersWhoDisliked()) {
            user.getDislikedVideos().remove(videoById);
        }

        for (User user : videoById.getUsersWhoWatched()) {
            user.getWatchedVideos().remove(videoById);
        }

        Path directory = Path.of(dataFolder, videoById.getId().toString());
        File folder = directory.toFile();
        deleteDirectory(folder);
    }

    @Transactional
    public void deleteVideoAsAdmin(Long id) {
        Video videoById = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cannot find video by id " + id));

        videoRepository.deleteById(id);

        for (User user : videoById.getUsersWhoLiked()) {
            user.getLikedVideos().remove(videoById);
        }

        for (User user : videoById.getUsersWhoDisliked()) {
            user.getDislikedVideos().remove(videoById);
        }

        for (User user : videoById.getUsersWhoWatched()) {
            user.getWatchedVideos().remove(videoById);
        }

        Path directory = Path.of(dataFolder, videoById.getId().toString());
        File folder = directory.toFile();
        deleteDirectory(folder);
    }

    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }

    public VideoPageDto findPageByTitle(String title, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Video> videos = videoRepository.findByTitleContaining(title, pageable);
        if (videos.hasContent()) {
            return new VideoPageDto(videos.stream().map(videoMapper::toDto).collect(Collectors.toList()), videos.getTotalPages());
        } else {
            return new VideoPageDto(null, videos.getTotalPages());
        }
    }

    public VideoPageDto findPageByTag(String tag, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Video> videos = videoRepository.findByTags(convertStringsToTags(Set.of(tag)).iterator().next(), pageable);
        if (videos.hasContent()) {
            return new VideoPageDto(videos.stream().map(videoMapper::toDto).collect(Collectors.toList()), videos.getTotalPages());
        } else {
            return new VideoPageDto(null, videos.getTotalPages());
        }
    }
}
