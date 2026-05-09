package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Announcements;
import edu.cqie.cqdemo.service.AnnouncementsService;
import edu.cqie.cqdemo.util.OSSOperationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

import static java.lang.Enum.valueOf;
import static org.jacoco.agent.rt.internal_43f5073.core.runtime.AgentOptions.OutputMode.file;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementsController {
    @Autowired
    private AnnouncementsService announcementsService;
    @Autowired
    private OSSOperationUtil ossOperationUtil;
    /**
     * 公告查询
     * @return
     */
    @RequestMapping("/queryByStatus")
    public Result<List<Announcements>> queryByStatus(Integer status) {
        if (status != null){
            List<Announcements> list = announcementsService.queryByStatus(status);
            return Result.success(list);
        }else {
            return Result.error("状态不能为空");
        }
    }
    /**
     * 添加公告
     * @return
     */
    @RequestMapping("/addAnnouncements")
    public Result addAnnouncements(
            Announcements announcements,
            MultipartFile file) {
        if (announcements != null && file != null){
            String imageUrl = ossOperationUtil.uploadNoticeImg( file);
            announcements.setCoverImg(imageUrl);

            boolean result = announcementsService.addAnnouncements(announcements);
            if ( result){
                return Result.success("添加成功");
            }else {
                return Result.error("添加失败");
            }
        }else {
            return Result.error("无文件传输或无数据");
        }
    }
    /**
     * 批量删除公告
     * @return
     */
    @RequestMapping("/deleteAnnouncements")
    public Result deleteAnnouncements(@RequestParam List<Integer> ids) {
        if (ids != null){
            boolean result = announcementsService.deleteAnnouncements(ids);
            if ( result){
                return Result.success("删除成功");
            }else {
                return Result.error("删除失败");
            }
        }else {
            return Result.error("无文件传输或无数据");
        }
    }
    @PostMapping("/publish")
    public Result publish(Integer id) {
        if (id != null){
            boolean result = announcementsService.publish(id);
            if ( result){
                return Result.success("发布成功");
            }else {
                return Result.error("发布失败");
            }
        }else {
            return Result.error("公告不存在！");
        }
    }

    /**
     * 修改公告信息
     * @return
     */
    @PostMapping("/updateAnnouncements")
    public Result updateAnnouncements(MultipartFile file, Announcements announcements){
        if (announcements != null && file != null){
            String imageUrl = ossOperationUtil.uploadNoticeImg(file);
            announcements.setCoverImg(imageUrl);

            boolean result = announcementsService.updateAnnouncements(announcements);
            if (result){
                return Result.success("修改成功");
            }else {
                return Result.error("修改失败");
            }
        }else {
            return Result.error("文件或公告数据信息为空");
        }
    }
}
