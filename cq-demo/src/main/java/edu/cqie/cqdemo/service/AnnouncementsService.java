package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.entity.Announcements;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author
* @description 针对表【announcements(公告表)】的数据库操作Service
* @createDate 2026-03-05 11:06:43
*/
public interface AnnouncementsService extends IService<Announcements> {
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
