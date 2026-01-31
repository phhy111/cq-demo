package edu.cqie.cqdemo.service;


import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.Routes;

import java.util.List;

/**
* @author
* @description 针对表【routes(路线表)】的数据库操作Service
* @createDate 2026-01-31 11:47:59
*/
public interface RoutesService extends IService<Routes> {
    public List<Routes> getRoutesListInfo();
}
