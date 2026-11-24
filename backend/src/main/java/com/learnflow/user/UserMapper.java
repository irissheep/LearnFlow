package com.learnflow.user;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("SELECT id, username, password_hash, nickname, created_at FROM app_user WHERE username = #{username}")
    UserAccount findByUsername(String username);

    @Select("SELECT id, username, password_hash, nickname, created_at FROM app_user WHERE id = #{id}")
    UserAccount findById(long id);

    @Insert("INSERT INTO app_user (username, password_hash, nickname) VALUES (#{username}, #{passwordHash}, #{nickname})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount user);
}
