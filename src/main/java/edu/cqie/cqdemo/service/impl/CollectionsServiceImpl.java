package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.mapper.CollectionsMapper;
import edu.cqie.cqdemo.service.CollectionsService;
import org.springframework.stereotype.Service;

@Service
public class CollectionsServiceImpl extends ServiceImpl<CollectionsMapper, Collections> implements CollectionsService {
}
