package com.buildrun.springsecurityjwt.controller;

import com.buildrun.springsecurityjwt.controller.dto.CreateTweetDto;
import com.buildrun.springsecurityjwt.controller.dto.FeedDto;
import com.buildrun.springsecurityjwt.controller.dto.FeedItemDto;
import com.buildrun.springsecurityjwt.entities.Role;
import com.buildrun.springsecurityjwt.entities.Tweet;
import com.buildrun.springsecurityjwt.entities.User;
import com.buildrun.springsecurityjwt.repository.TweetRepository;
import com.buildrun.springsecurityjwt.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@RestController
public class TweetController {

    private final TweetRepository tweetRepository;
    private final UserRepository userRepository;

    public TweetController(TweetRepository tweetRepository, UserRepository userRepository) {
        this.tweetRepository = tweetRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/feed")
    public ResponseEntity<FeedDto> feed(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize
    ){
        var tweets = tweetRepository.findAll(PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "creationTimestamp")))
                .map(tweet -> new FeedItemDto(tweet.getTweetId(), tweet.getContent(), tweet.getUser().getUsername()));
        return ResponseEntity.ok(new FeedDto(tweets.getContent(), page, pageSize, tweets.getTotalPages(), tweets.getTotalElements()));
    }


    @PostMapping("/tweets")
    public ResponseEntity<Void> createTweet(
            @RequestBody CreateTweetDto dto,
            JwtAuthenticationToken token) {

        var user = userRepository.findById(UUID.fromString(token.getName()));

        Tweet tweet = new Tweet();
        tweet.setUser(user.get());
        tweet.setContent(dto.content());

        tweetRepository.save(tweet);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tweets/{tweetId}")
    public ResponseEntity<Void> deleteTweet(
            @PathVariable long tweetId,
            JwtAuthenticationToken token) {

        var user = userRepository.findById(UUID.fromString(token.getName()));


        var tweet = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var isAdmin = user.get().getRoles()
                .stream()
                .anyMatch(role -> role.getName().equals(Role.Values.ADMIN.name()));

        if(isAdmin || tweet.getUser().getUserId().equals(UUID.fromString(token.getName()))) {
            tweetRepository.delete(tweet);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok().build();
    }

}
