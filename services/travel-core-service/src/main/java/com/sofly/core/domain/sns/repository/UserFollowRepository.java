package com.sofly.core.domain.sns.repository;

import com.sofly.core.domain.sns.entity.UserFollow;
import com.sofly.core.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowingId(Long followingId); // 나를 팔로우하는 수 = 팔로워 수

    long countByFollowerId(Long followerId); // 내가 팔로우하는 수 = 팔로잉 수

    // 팔로워 목록 (나를 팔로우하는 사람들)
    @Query("SELECT uf.follower FROM UserFollow uf WHERE uf.following.id = :userId")
    List<User> findFollowersByUserId(@Param("userId") Long userId);

    // 팔로잉 목록 (내가 팔로우하는 사람들)
    @Query("SELECT uf.following FROM UserFollow uf WHERE uf.follower.id = :userId")
    List<User> findFollowingsByUserId(@Param("userId") Long userId);

    @Query("SELECT uf.following.id FROM UserFollow uf WHERE uf.follower.id = :followerId")
    List<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
