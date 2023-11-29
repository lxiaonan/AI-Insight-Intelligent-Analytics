package com.xiaonan.xnbi.esdao;

import com.xiaonan.xnbi.model.dto.post.PostEsDTO;
import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 帖子 ES 操作
 *
 * @author <a href="https://github.com/lixiaonan">小楠</a>
 * 
 */
public interface PostEsDao extends ElasticsearchRepository<PostEsDTO, Long> {

    List<PostEsDTO> findByUserId(Long userId);
}
