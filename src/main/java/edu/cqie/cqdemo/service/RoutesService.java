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

    //routes的所有status是1的升序
    List<Routes> getRoutesListInfoTimeS();
    //routes的所有status是1的降序
    List<Routes> getRoutesListInfoTimeJ();
}
