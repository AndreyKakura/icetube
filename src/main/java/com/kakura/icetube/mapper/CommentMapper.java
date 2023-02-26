package com.kakura.icetube.mapper;

import com.kakura.icetube.dto.CommentDto;
import com.kakura.icetube.model.Comment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommentMapper {
    public Comment toModel(CommentDto commentDto) {
        Comment comment = Comment.builder()
                .text(commentDto.getText())
                .createdAt(LocalDateTime.now())
                .build();
        return comment;
    }

    public CommentDto toDto(Comment comment) {
        CommentDto commentDto = CommentDto.builder()
                .text(comment.getText())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .createdAt(comment.getCreatedAt())
                .build();
        return commentDto;
    }
}
