package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.ScenicsAndRegionDTO;
import edu.cqie.cqdemo.dto.ScenicsDTO;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.service.ScenicsService;
import edu.cqie.cqdemo.util.OSSOperationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/scenics")
public class ScenicsController {
    /**
     * 查询景点信息
     * @return
     */
    @Autowired
    private ScenicsService scenicsService;
    /**
     * 获取所有景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfo")
    public Result<List<Scenics>> getScenicsInfo() {
       List<Scenics> scenicsList = scenicsService.list();
       if (scenicsList != null){
           return Result.success(scenicsList);
       }else {
           return Result.error("未查询到景点信息");
       }
    }
    /**
     * 获取轮播图信息
     * @return
     */
    @GetMapping("/GetSlideShowInfo")
    public Result<List<Scenics>> getSlideShowInfo() {
        List<Scenics> scenicsList = scenicsService.getSlideShowInfo();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到轮播图信息");
        }
    }

    /**
     * 获取对应区域的景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfoByRegionId")
    public Result<List<Scenics>> getScenicsInfoByRegionId(Integer regionId) {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByRegionId(regionId);
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该区域下的景点信息");
        }
    }
    /**
     * 获取不同等级的景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfoByLevel")
    public Result<List<Scenics>> getScenicsInfoByLevel(String level) {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByLevel(level);
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该等级的景点信息");
        }
    }
    /**
     * 按评分高低获取景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfoByScore")
    public Result<List<Scenics>> getScenicsInfoByScore() {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByScore();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该评分的景点信息");
        }
    }
    /**
     * 按点赞量排序
     * @return
     */
    @GetMapping("/GetScenicsInfoByLike")
    public Result<List<Scenics>> getScenicsInfoByLike() {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByLikeCount();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该点赞量的景点信息");
        }
    }
    /**
     * 添加景点浏览量
     * @return
     */
    @PostMapping("/AddViewCount")
    public Result<String> addViewCount(Integer id) {
        boolean result = scenicsService.addViewCount(id);
        if (result){
            return Result.success("添加成功");
        }else {
            return Result.error("添加失败");
        }
    }
    /**
     * 获取景点详情信息
     * @return
     */
    @GetMapping("/GetScenicsDetailInfoById")
    public Result<List<ScenicsDTO>> getScenicsDetailInfoById(Integer id) {
        List<ScenicsDTO> scenicsDTOList = scenicsService.getScenicsDetailInfoById(id);
        if (scenicsDTOList != null){
            return Result.success(scenicsDTOList);
        }else {
            return Result.error("未查询到该景点详情信息");
        }
    }
    /**
     * 多表关联分页查询
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/selectScenicsAndRegionPage")
    public Result selectScenicsAndRegionPage(
            @RequestParam(defaultValue = "1") Integer pageNum, // 默认第1页
            @RequestParam(defaultValue = "5") Integer pageSize) // 默认每页5条
    {
        IPage<ScenicsAndRegionDTO> page = scenicsService.selectScenicsAndRegionPage(pageNum, pageSize);
        if (page != null){
            return Result.success(page);
        }else {
            return Result.error("多表关联分页查询失败");
        }
    }
    /**
     * 添加景点信息
     * @return
     */
    @Autowired
    private OSSOperationUtil ossOperationUtil;
    @PostMapping("/AddScenicsInfo")
    public Result addScenicsInfo(
            MultipartFile file,
            Scenics scenics) {
        if (file != null) {
            //获取生成的路径
            String imageUrl = ossOperationUtil.uploadScenicsCoverImg(file);
            scenics.setCoverImg(imageUrl);

            boolean result = scenicsService.addScenicsInfo(scenics);
            if (result) {
                return Result.success("添加成功");
            } else {
                return Result.error("添加失败");
            }
        } else {
            System.out.println("图片为空");
            return Result.error("图片为空");
        }
    }
    /**
     * 批量删除景点信息
     * @return
     */
    @PostMapping("/DeleteScenics")
    public Result deleteScenics(@RequestParam("id") List<Integer>  id){
        if (id != null){
            boolean result = scenicsService.removeByIds(id);
            if (result){
                return Result.success("删除成功");
            }else {
                return Result.error("删除失败");
            }
        }else {
            return Result.error("删除失败");
        }
    }
    /**
     * 修改景点信息
     * @return
     */
    @PostMapping("/UpdateScenicsInfo")
    public Result updateScenicsInfo(MultipartFile file, Scenics scenics){
        if (scenics != null && file != null){
            String imageUrl = ossOperationUtil.uploadScenicsCoverImg(file);
            scenics.setCoverImg(imageUrl);

            boolean result = scenicsService.updateScenicsInfo(scenics);
            if (result){
                return Result.success("修改成功");
            }else {
                return Result.error("修改失败");
            }
        }else {
            return Result.error("文件或景点数据信息为空");
        }
    }
}
