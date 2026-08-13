package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
