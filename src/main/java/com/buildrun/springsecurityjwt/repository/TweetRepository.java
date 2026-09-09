package com.buildrun.springsecurityjwt.repository;

import com.buildrun.springsecurityjwt.entities.Tweet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {
}
