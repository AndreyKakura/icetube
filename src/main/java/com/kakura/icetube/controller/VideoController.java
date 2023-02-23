package com.kakura.icetube.controller;

import com.kakura.icetube.dto.*;
import com.kakura.icetube.exception.NotFoundException;
import com.kakura.icetube.service.StreamBytesInfo;
import com.kakura.icetube.service.VideoService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
    public List<VideoDto> findAll() {
        return videoService.findAll();
    }

    @GetMapping("/{id}")
    public VideoDto findById(@PathVariable("id") Long id) {
        return videoService.findById(id);
    }

    @GetMapping(value = "/preview/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<ByteArrayResource> getPreviewPicture(@PathVariable("id") Long id) {
        byte[] imageBytes = videoService.getPreviewBytes(id);
        ByteArrayResource resource = new ByteArrayResource(imageBytes);
        return ResponseEntity.ok().contentLength(imageBytes.length).body(resource);
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<StreamingResponseBody> streamVideo(@RequestHeader(value = "Range", required = false) String httpRangeHeader,
                                                             @PathVariable("id") Long id) {
        List<HttpRange> httpRangeList = HttpRange.parseRanges(httpRangeHeader);
        StreamBytesInfo streamBytesInfo = videoService.getStreamBytes(id, httpRangeList.size() > 0 ? httpRangeList.get(0) : null)
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

    @PostMapping(path = "/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> uploadVideo(@ModelAttribute @Valid NewVideoDto newVideoDto) {
        VideoDto videoDtoResponse = null;
        try {
            videoDtoResponse = videoService.saveNewVideo(newVideoDto);
        } catch (Exception ex) {
            log.error(ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(videoDtoResponse);

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
    public void addComment(@PathVariable("id") Long id, @RequestBody CommentDto commentDto) {
        videoService.addComment(id, commentDto);
    }

    @GetMapping("/{id}/comment")
    public List<CommentDto> getAllComments(@PathVariable("id") Long id) {
       return videoService.getAllComments(id);
    }

    @GetMapping("/history")
    public Set<VideoDto> getVideoHistory() {
        return videoService.getVideoHistory();
    }

}
