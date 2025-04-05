package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User?, Int?> {
    //    @Query("SELECT u FROM User u WHERE u.country is null")
    //    List<User> findAllWithoutCountry();
    fun findByEmail(email: String): User?

    fun findByEmailAndSaltPasswordHash(email: String, saltPasswordHash: String): User?
    //    User findByUsername(String username);
    //    List<User> findByUsernameIn(List<String> usernames);
}