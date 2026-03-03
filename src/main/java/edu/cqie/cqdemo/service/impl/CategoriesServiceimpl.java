package edu.cqie.cqdemo.service.impl;

import edu.cqie.cqdemo.entity.Categories;
import edu.cqie.cqdemo.mapper.CategoriesMapper;
import edu.cqie.cqdemo.service.CategoriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class CategoriesServiceimpl implements CategoriesService {
    @Autowired
    private CategoriesMapper categoriesMapper;

    @Override
    public List<Categories> selectcategories() {
        List<Categories>categories=categoriesMapper.selectcategories();
        return categories;
    }
}
