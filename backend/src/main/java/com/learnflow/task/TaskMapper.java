package com.learnflow.task;
import java.util.List;
import com.learnflow.stats.TaskCounts;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TaskMapper {
    @Insert("INSERT INTO learning_task (user_id, title, status) VALUES (#{userId}, #{title}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Task task);

    // 归属条件放进 SQL：即使用户猜到别人的 id，也查不到、改不了、删不了。
    @Select("SELECT id, user_id, title, status, created_at, updated_at FROM learning_task WHERE id = #{id} AND user_id = #{userId}")
    Task findById(@Param("id") long id, @Param("userId") long userId);

    @Select("SELECT id, user_id, title, status, created_at, updated_at FROM learning_task WHERE user_id = #{userId} ORDER BY id DESC")
    List<Task> findAll(long userId);

    @Update("UPDATE learning_task SET title = #{title}, status = #{status} WHERE id = #{id} AND user_id = #{userId}")
    int update(Task task);

    @Delete("DELETE FROM learning_task WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") long id, @Param("userId") long userId);

    @Select("""
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END), 0) AS done,
                   COALESCE(SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END), 0) AS in_progress
            FROM learning_task WHERE user_id = #{userId}
            """)
    TaskCounts counts(long userId);
}
