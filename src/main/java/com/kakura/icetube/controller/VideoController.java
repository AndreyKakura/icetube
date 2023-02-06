package com.kakura.icetube.controller;

import com.kakura.icetube.dto.EditVideoDto;
import com.kakura.icetube.dto.NewVideoDto;
import com.kakura.icetube.dto.UploadVideoResponse;
import com.kakura.icetube.dto.VideoDto;
import com.kakura.icetube.service.StreamBytesInfo;
import com.kakura.icetube.service.VideoService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;

@RestController
@RequestMapping("/api/v1/video")
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
        return videoService.findById(id).orElseThrow(NotFoundException::new);
    }

    @GetMapping(value = "/preview/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<StreamingResponseBody> getPreviewPicture(@PathVariable("id") Long id) {
        InputStream inputStream = videoService.getPreviewInputStream(id)
                .orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(inputStream::transferTo);
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<StreamingResponseBody> streamVideo(@RequestHeader(value = "Range", required = false) String httpRangeHeader,
                                                             @PathVariable("id") Long id) {
        List<HttpRange> httpRangeList = HttpRange.parseRanges(httpRangeHeader);
        StreamBytesInfo streamBytesInfo = videoService.getStreamBytes(id, httpRangeList.size() > 0 ? httpRangeList.get(0) : null)
                .orElseThrow(NotFoundException::new);

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
    public ResponseEntity<UploadVideoResponse> uploadVideo(@ModelAttribute @Valid NewVideoDto newVideoDto) {
        UploadVideoResponse uploadVideoResponse = null;
        try {
            uploadVideoResponse = videoService.saveNewVideo(newVideoDto);
        } catch (Exception ex) {
            log.error(ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadVideoResponse);

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

    @ExceptionHandler
    public ResponseEntity<Void> notFoundExceptionHandler(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

}
