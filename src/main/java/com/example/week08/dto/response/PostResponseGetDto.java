//package com.example.week08.dto.response;
//
//import com.example.week08.domain.Post;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//@Builder
//@Getter
//@NoArgsConstructor
//@AllArgsConstructor
//public class PostResponseGetDto {
//    private Long id;
//    private String title;
//    private String content;
//    private String image;
//    private String weather;
//    private String region;
//    private String season;
//    private String who;
//    private double avgScore;
//    private int heart;
//    private LocalDateTime createdAt;
//    private LocalDateTime modifiedAt;
//
//
//    public PostResponseGetDto(Post post) {
//        this.id = post.getId();
//        this.title = post.getTitle();
//        this.content = post.getContent();
//        this.image = post.getImage();
//        this.weather = post.getWeather();
//        this.region = post.getRegion();
//        this.season = post.getSeason();
//        this.who = post.getWho();
//        this.avgScore = post.getAvgScore();
//        this.heart = post.getHeart();
//        this.createdAt = post.getCreatedAt();
//        this.modifiedAt = post.getModifiedAt();
//    }
//}
