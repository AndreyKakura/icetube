package com.kakura.icetube.repository;

import com.kakura.icetube.model.Subscription;
import com.kakura.icetube.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberAndSubscribedTo(User subscriber, User subscribedTo);

}
