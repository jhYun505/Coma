package com.coma.coma.users.repository;

import com.coma.coma.users.domain.Users;
import com.coma.coma.users.dto.UserResponseDto;

import java.util.Optional;

public interface UserRepository {
    UserResponseDto getUserByUserId(int id);
    Optional<Users> findByUserIdName(String id);

    void save(Users user);  // 사용자 저장 메서드 추가
    boolean existsById(String id); // 아이디 중복체크 메서드
    void update(Users user);
    void delete(Users user);
}
