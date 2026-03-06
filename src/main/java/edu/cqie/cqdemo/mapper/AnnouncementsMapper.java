package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.Announcements;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author
* @description 针对表【announcements(公告表)】的数据库操作Mapper
* @createDate 2026-03-05 11:06:43
* @Entity edu.cqie.cqdemo.entity.Announcements
*/
@Mapper
public interface AnnouncementsMapper extends BaseMapper<Announcements> {
    /**
     * 公告查询
     */
    List<Announcements> queryByStatus(Integer status);
    /**
     * 新增公告
     */
    boolean addAnnouncements(Announcements announcements);
    /**
     * 批量删除公告
     */
    boolean deleteAnnouncements(List<Integer> ids);
    /**
     * 发布公告
     */
    boolean publish(Integer id);
    /**
     * 修改公告
     */
    boolean updateAnnouncements(Announcements announcements);

}




