package com.kakura.icetube.controller;

import com.kakura.icetube.dto.*;
import com.kakura.icetube.exception.BadRequestException;
import com.kakura.icetube.exception.NotFoundException;
import com.kakura.icetube.service.StreamBytesInfo;
import com.kakura.icetube.service.VideoService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/video")
@Log4j2
@AllArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping()
    public VideoPageDto findPage(@RequestParam(defaultValue = "0") Integer pageNumber,
                                 @RequestParam(defaultValue = "12") Integer pageSize) {

        System.out.println("aaaaaaaa");

        if (pageNumber < 0 || pageSize < 1) {
            throw new BadRequestException("Bad request");
        }

        return videoService.findPage(pageNumber, pageSize);
    }

    @GetMapping(value = "/publishedby/{userId}")
    public VideoPageDto findPublishedByUserPage(@PathVariable("userId") Long userId, @RequestParam(defaultValue = "0") Integer pageNumber,
                                                @RequestParam(defaultValue = "12") Integer pageSize) {
        return videoService.findPublishedByUserIdPage(userId, pageNumber, pageSize);
    }

    @GetMapping("/subscriptions")
    public VideoPageDto getSubscribedVideos(@RequestParam(defaultValue = "0") Integer pageNumber,
                                            @RequestParam(defaultValue = "12") Integer pageSize) {
        return videoService.getSubscribedVideosPage(pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    public VideoDto findById(@PathVariable("id") Long id) {
        return videoService.findById(id);
    }

    @GetMapping(value = "/preview/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    @Cacheable(value = "previews", key = "#id")
    public ResponseEntity<ByteArrayResource> getPreviewPicture(@PathVariable("id") Long id) {
        byte[] imageBytes = videoService.getPreviewBytes(id);
        ByteArrayResource resource = new ByteArrayResource(imageBytes);
        return ResponseEntity.ok().contentLength(imageBytes.length).body(resource);
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<StreamingResponseBody> streamVideo(@RequestHeader(value = "Range", required = false) String httpRangeHeader,
                                                             @PathVariable("id") Long id,
                                                             @RequestParam("quality") String quality) {
        List<HttpRange> httpRangeList = HttpRange.parseRanges(httpRangeHeader);
        StreamBytesInfo streamBytesInfo = videoService.getStreamBytes(id, httpRangeList.size() > 0 ? httpRangeList.get(0) : null, quality)
                .orElseThrow(() -> new NotFoundException("Cannot get stream bytes"));

        long byteLength = streamBytesInfo.getRangeEnd() - streamBytesInfo.getRangeStart() + 1;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(httpRangeList.size() > 0 ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header("Content-Type", streamBytesInfo.getContentType())
                .header("Accept-Ranges", "bytes")
                .header("Content-Length", Long.toString(byteLength));

        if (httpRangeList.size() > 0) {
            builder.header("Content-Range",
                    "bytes " + streamBytesInfo.getRangeStart() +
                            "-" + streamBytesInfo.getRangeEnd() +
                            "/" + streamBytesInfo.getFileSize());
        }
        log.info("Providing bytes from {} to {}. We are at {}% of overall video.",
                streamBytesInfo.getRangeStart(), streamBytesInfo.getRangeEnd(),
                new DecimalFormat("###.##").format(100.0 * streamBytesInfo.getRangeStart() / streamBytesInfo.getFileSize()));
        return builder.body(streamBytesInfo.getResponseBody());

    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping(path = "/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> uploadVideo(@ModelAttribute @Valid NewVideoDto newVideoDto) {
//        VideoDto videoDtoResponse = null;
        try {
//            videoDtoResponse = videoService.saveNewVideo(newVideoDto);
            videoService.saveNewVideo(newVideoDto);
        } catch (Exception ex) {
            log.error(ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    @PostMapping("/preview")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadPreview(@RequestParam("file") MultipartFile file, @RequestParam("videoId") Long videoId) {
        videoService.uploadPreview(file, videoId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public EditVideoDto editVideoMetadata(@RequestBody EditVideoDto editVideoDto) {
        return videoService.editVideo(editVideoDto);
    }

    @PostMapping("/{id}/like")
    public VideoDto likeVideo(@PathVariable("id") Long id) {
        return videoService.likeVideo(id);
    }

    @PostMapping("/{id}/dislike")
    public VideoDto dislikeVideo(@PathVariable("id") Long id) {
        return videoService.dislikeVideo(id);
    }

    @PostMapping("/{id}/comment")
    public void addComment(@PathVariable("id") Long id, @RequestBody @Valid CommentDto commentDto) {
        videoService.addComment(id, commentDto);
    }

    @GetMapping("/{id}/comment")
    public List<CommentDto> getAllComments(@PathVariable("id") Long id) {
        return videoService.getAllComments(id);
    }

    @GetMapping("/history")
    public VideoPageDto getVideoHistory(@RequestParam(defaultValue = "0") Integer pageNumber,
                                        @RequestParam(defaultValue = "12") Integer pageSize) {
        return videoService.getVideoHistoryPage(pageNumber, pageSize);
    }

    @GetMapping(value = "/liked")
    public VideoPageDto getLiked(@RequestParam(defaultValue = "0") Integer pageNumber,
                                 @RequestParam(defaultValue = "12") Integer pageSize) {
        return videoService.getLikedVideos(pageNumber, pageSize);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadVideo(@PathVariable("id") Long id, @RequestParam("quality") String quality) {
        return videoService.downloadVideo(id, quality);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable("id") Long id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok().build();
    }


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/admin-delete/{id}")
    public ResponseEntity<?> deleteVideoAsAdmin(@PathVariable("id") Long id) {
        videoService.deleteVideoAsAdmin(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/findbytitle")
    public VideoPageDto findPageByTitle(@RequestParam(defaultValue = "0") Integer pageNumber,
                                        @RequestParam(defaultValue = "12") Integer pageSize,
                                        @RequestParam String title) {
        System.out.println("ffffff");
        System.out.println(videoService.findPageByTitle(title, pageNumber, pageSize));
        return videoService.findPageByTitle(title, pageNumber, pageSize);
    }

    @GetMapping("/findbytag")
    public VideoPageDto findPageByTag(@RequestParam(defaultValue = "0") Integer pageNumber,
                                      @RequestParam(defaultValue = "12") Integer pageSize,
                                      @RequestParam String tag) {
        return videoService.findPageByTag(tag, pageNumber, pageSize);
    }
}
