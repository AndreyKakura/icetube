package com.kakura.icetube.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscriptions",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"subscriber_id", "subscribed_to_id"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    private User subscribedTo;
}
