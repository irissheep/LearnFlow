package com.learnflow.note;
import java.time.LocalDateTime;
import java.util.List;
import com.learnflow.ai.NoteManifest;
import org.apache.ibatis.annotations.*;

@Mapper
public interface NoteMapper {
    @Insert("INSERT INTO knowledge_note (user_id, title, category, content) VALUES (#{userId}, #{title}, #{category}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Note note);

    @Select("SELECT * FROM knowledge_note WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL")
    Note findById(@Param("id") long id, @Param("userId") long userId);

    @Select("""
        <script>
        SELECT * FROM knowledge_note WHERE user_id = #{userId} AND deleted_at IS NULL
        <if test="keyword != null and keyword != ''">
            AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="category != null and category != ''">AND category = #{category}</if>
        ORDER BY updated_at DESC, id DESC LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<Note> findPage(@Param("userId") long userId, @Param("keyword") String keyword,
            @Param("category") String category, @Param("offset") int offset, @Param("size") int size);

    @Select("""
        <script>
        SELECT COUNT(*) FROM knowledge_note WHERE user_id = #{userId} AND deleted_at IS NULL
        <if test="keyword != null and keyword != ''">
            AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="category != null and category != ''">AND category = #{category}</if>
        </script>
        """)
    long count(@Param("userId") long userId, @Param("keyword") String keyword, @Param("category") String category);

    @Select("SELECT DISTINCT category FROM knowledge_note WHERE user_id = #{userId} AND deleted_at IS NULL ORDER BY category")
    List<String> categories(long userId);

    // 内容变更与 PENDING 标记在同一事务里提交：Python 暂时不可用也不会丢失待索引任务。
    @Update("""
        UPDATE knowledge_note SET title = #{title}, category = #{category}, content = #{content},
            revision = revision + 1, index_status = 'PENDING', index_error = NULL,
            index_attempts = 0, next_index_at = CURRENT_TIMESTAMP(6)
        WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL
        """)
    int update(Note note);

    // 软删除保留索引清理任务；正常查询立刻看不到这条笔记。
    @Update("""
        UPDATE knowledge_note SET deleted_at = CURRENT_TIMESTAMP(6), revision = revision + 1,
            index_status = 'PENDING', index_error = NULL, index_attempts = 0,
            next_index_at = CURRENT_TIMESTAMP(6)
        WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL
        """)
    int delete(@Param("id") long id, @Param("userId") long userId);

    @Update("""
        UPDATE knowledge_note SET revision = revision + 1, index_status = 'PENDING', index_error = NULL,
            index_attempts = 0, next_index_at = CURRENT_TIMESTAMP(6), updated_at = updated_at
        WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL
        """)
    int retry(@Param("id") long id, @Param("userId") long userId);

    @Update("""
        UPDATE knowledge_note SET revision = revision + 1, index_status = 'PENDING', index_error = NULL,
            index_attempts = 0, next_index_at = CURRENT_TIMESTAMP(6), updated_at = updated_at
        WHERE user_id = #{userId} AND deleted_at IS NULL
        """)
    int reindexAll(long userId);

    @Select("SELECT * FROM knowledge_note WHERE index_status IN ('PENDING','FAILED') AND next_index_at <= CURRENT_TIMESTAMP(6) ORDER BY next_index_at, id LIMIT 10")
    List<Note> pending();

    @Update("""
        UPDATE knowledge_note SET index_status = 'READY', index_error = NULL, index_attempts = 0,
            next_index_at = NULL, updated_at = updated_at WHERE id = #{id} AND revision = #{revision}
        """)
    int indexed(@Param("id") long id, @Param("revision") long revision);

    @Update("""
        UPDATE knowledge_note SET index_status = 'FAILED', index_error = #{error},
            index_attempts = #{attempts}, next_index_at = #{nextAt}, updated_at = updated_at
        WHERE id = #{id} AND revision = #{revision}
        """)
    int failed(@Param("id") long id, @Param("revision") long revision, @Param("error") String error,
            @Param("attempts") int attempts, @Param("nextAt") LocalDateTime nextAt);

    @Select("SELECT id AS note_id, revision FROM knowledge_note WHERE user_id = #{userId} AND deleted_at IS NULL")
    List<NoteManifest> manifest(long userId);
}
