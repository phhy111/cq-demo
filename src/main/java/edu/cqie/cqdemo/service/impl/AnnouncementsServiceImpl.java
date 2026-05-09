package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.Announcements;
import edu.cqie.cqdemo.service.AnnouncementsService;
import edu.cqie.cqdemo.mapper.AnnouncementsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author
* @description 针对表【announcements(公告表)】的数据库操作Service实现
* @createDate 2026-03-05 11:06:43
*/
@Service
public class AnnouncementsServiceImpl extends ServiceImpl<AnnouncementsMapper, Announcements>
    implements AnnouncementsService{

    @Autowired
    private AnnouncementsMapper announcementsMapper;
    @Override
    public List<Announcements> queryByStatus(Integer status) {
        return announcementsMapper.queryByStatus( status);
    }

    @Override
    public boolean addAnnouncements(Announcements announcements) {
        return announcementsMapper.addAnnouncements(announcements);
    }

    @Override
    public boolean deleteAnnouncements(List<Integer> ids) {
        return announcementsMapper.deleteAnnouncements(ids);
    }

    @Override
    public boolean publish(Integer id) {
        return announcementsMapper.publish(id);
    }

    @Override
    public boolean updateAnnouncements(Announcements announcements) {
        return announcementsMapper.updateAnnouncements(announcements);
    }
}




