package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.entity.UserFollow;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.sofly.core.domain.sns.code.SnsErrorCode.*;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    //팔로우하기
    @Transactional
    public void follow(Long followerId, Long followingId){

        if (followerId.equals(followingId)){
            throw new SnsException(CANNOT_FOLLOW_SELF);
        }

        if (userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)){
            throw new SnsException(ALREADY_FOLLOWING);
        }

        User follower = getUserOrThrow(followerId);
        User following = getUserOrThrow(followingId);

        UserFollow userFollow = UserFollow.builder()
                .follower(follower)
                .following(following)
                .build();

        userFollowRepository.save(userFollow);
    }

    //언팔하기
    @Transactional
    public void unfollow(Long followerId, Long followingId){

        if (!userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)){
            throw new SnsException(FOLLOW_NOT_FOUND);
        }

        userFollowRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    //팔로워 수
    public long getFollowerCount(Long userId){
        return userFollowRepository.countByFollowingId(userId);
    }

    //팔로잉 수
    public long getFollowingCount(Long userId){
        return userFollowRepository.countByFollowerId(userId);
    }

    //팔로잉  list
    public List<String> getFollowingList(Long userId){
        return userFollowRepository.findFollowingsByUserId(userId)
                .stream()
                .map(User::getNickname) //Todo 나중에 profile 기능 들어오면 profile에 필요한 정보들 넘겨줘야됨
                .toList();
    }

    //팔로워 list
    public List<String> getFollowerList(Long userId){
        return userFollowRepository.findFollowersByUserId(userId)
                .stream()
                .map(User::getNickname)
                .toList(); //Todo 나중에 profile 기능 들어오면 profile에 필요한 정보들 넘겨줘야됨
    }

    //팔로우 여부 확인
    public boolean isFollowing(Long followerId, Long followingId){
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    //맞팔 여부 확인
    boolean isMutualFollow(Long userId, Long targetId){
        return isFollowing(userId, targetId) && isFollowing(targetId, userId);
    }

    // ------------내부 함수 ------------------------------------------------------
    private User getUserOrThrow(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다. id=" + userId));
    }
}
