package com.kakura.icetube.service;

import com.kakura.icetube.dto.CommentDto;
import com.kakura.icetube.exception.NotFoundException;
import com.kakura.icetube.mapper.CommentMapper;
import com.kakura.icetube.mapper.VideoMapper;
import com.kakura.icetube.model.Comment;
import com.kakura.icetube.model.User;
import com.kakura.icetube.repository.CommentRepository;
import com.kakura.icetube.repository.TagRepository;
import com.kakura.icetube.repository.VideoRepository;
import com.kakura.icetube.dto.EditVideoDto;
import com.kakura.icetube.dto.NewVideoDto;
import com.kakura.icetube.dto.VideoDto;
import com.kakura.icetube.model.Tag;
import com.kakura.icetube.model.Video;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kakura.icetube.util.VideoUtil.removeFileExt;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

@Service
@Log4j2
public class VideoService {

//    public void uploadVideo(MultipartFile file) {
//        //TODO upload file
//    }

    @Value("${data.folder}")
    private String dataFolder;

    private final VideoRepository videoRepository;

    private final TagRepository tagRepository;

    private final FrameGrabberService frameGrabberService;

    private final VideoMapper videoMapper;

    private final UserService userService;

    private final CommentMapper commentMapper;

    private final CommentRepository commentRepository;

    @Autowired
    public VideoService(VideoRepository videoRepository, TagRepository tagRepository, FrameGrabberService frameGrabberService, VideoMapper videoMapper, UserService userService, CommentMapper commentMapper, CommentRepository commentRepository) {
        this.videoRepository = videoRepository;
        this.tagRepository = tagRepository;
        this.frameGrabberService = frameGrabberService;
        this.videoMapper = videoMapper;
        this.userService = userService;
        this.commentMapper = commentMapper;
        this.commentRepository = commentRepository;
    }


    public List<VideoDto> findAll() {
        return videoRepository.findAll().stream().map(videoMapper::toDto).collect(Collectors.toList());
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
        }


        return videoDto;
    }

    @Transactional
    public VideoDto saveNewVideo(NewVideoDto newVideoDto) {
        Video video = videoMapper.toModel(newVideoDto);
        if (newVideoDto.getTags() != null) {
            video.setTags(convertStringsToTags(newVideoDto.getTags()));
        }
        video.setUser(userService.getCurrentUser());

        Video savedVideo = videoRepository.save(video);

        Path directory = Path.of(dataFolder, video.getId().toString());
        try {
            Files.createDirectory(directory);
            Path videoPath = Path.of(directory.toString(), newVideoDto.getVideoFile().getOriginalFilename());
            try (OutputStream out = Files.newOutputStream(videoPath, CREATE, WRITE)) {
                newVideoDto.getVideoFile().getInputStream().transferTo(out);
            }

            Path previewPath = Path.of(directory.toString(), newVideoDto.getPreviewFile().getOriginalFilename());
            try (OutputStream out = Files.newOutputStream(previewPath, CREATE, WRITE)) {
                newVideoDto.getPreviewFile().getInputStream().transferTo(out);
            }
//            frameGrabberService.generatePreviewPictures(file); //TODO if preview from front is null then generate preview
            long length = frameGrabberService.lengthInTime(videoPath);
            video.setVideoLength(length);
            return videoMapper.toDto(videoRepository.save(video));
        } catch (IOException e) {
            log.error("", e);
            throw new IllegalStateException();
        }
//        return new UploadVideoResponse(savedVideo.getId(), "http://localhost:8080/api/v1/video/stream/" + savedVideo.getId());
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

    public Optional<StreamBytesInfo> getStreamBytes(Long id, HttpRange range) {
        Optional<Video> byId = videoRepository.findById(id);
        if (byId.isEmpty()) {
            return Optional.empty();
        }
        Path filePath = Path.of(dataFolder, Long.toString(id), byId.get().getVideoFileName());
        if (!Files.exists(filePath)) {
            log.error("File {} not found", filePath);
            return Optional.empty();
        }
        try {
            long fileSize = Files.size(filePath);
            long chunkSize = fileSize / 100;
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
        videoFromDb.setVideoStatus(editVideoDto.getVideoStatus()); //TODO
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

    public Set<VideoDto> getVideoHistory() {
        User currentUser = userService.getCurrentUser();
        return currentUser.getWatchedVideos().stream().map(videoMapper::toDto).collect(Collectors.toSet());
    }

}
