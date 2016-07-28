package com.zhuhuibao.mybatis.oms.service;

/**
 * Created by cxx on 2016/3/14 0014.
 */

import com.zhuhuibao.common.Response;
import com.zhuhuibao.common.constant.Constants;
import com.zhuhuibao.common.pojo.ResultBean;
import com.zhuhuibao.common.pojo.SysBean;
import com.zhuhuibao.mybatis.oms.entity.Category;
import com.zhuhuibao.mybatis.oms.mapper.CategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类目管理
 * 
 * @author cxx
 *
 */
@Service
@Transactional
public class CategoryService {
	private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

	@Autowired
	private CategoryMapper categoryMapper;

	/**
	 * 查询品牌所属大系统
	 */
	public List<ResultBean> findSystemByBrand(String id) {
		try {
			return categoryMapper.findSystemByBrand(id);
		} catch (Exception e) {
			log.error("findSystemByBrand error,id=" + id + " >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 查询品牌所属大系统,子系统
	 */
	public List<SysBean> findCategoryByBrand(String id) {
		try {
			return categoryMapper.findCategoryByBrand(id);
		} catch (Exception e) {
			log.error("findCategoryByBrand error,id=" + id + " >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 查询所有大系统
	 */
	public List<ResultBean> findSystemList() {
		try {
			return categoryMapper.findSystemList();
		} catch (Exception e) {
			log.error("findSystemList error >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 查询所有大系统,子系统
	 */
	public List<SysBean> searchAll() {
		try {
			return categoryMapper.searchAll();
		} catch (Exception e) {
			log.error("searchAll error >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 根据大系统id查询子系统
	 */
	public List<ResultBean> findSubSystemList(String id) {
		try {
			return categoryMapper.findSubSystemList(id);
		} catch (Exception e) {
			log.error("findSubSystemList error,id=" + id + " >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 根据大系统id查询子系统(limit 8)
	 */
	public List<ResultBean> findSubSystemListLimit(String id) {
		try {
			return categoryMapper.findSubSystemListLimit(id);
		} catch (Exception e) {
			log.error("findSubSystemListLimit error,id=" + id + " >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 根据大系统id查询大系统信息
	 */
	public Category findSystem(String id) {
		try {
			return categoryMapper.findSystem(id);
		} catch (Exception e) {
			log.error("findSystem error,id=" + id + " >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 添加类目
	 */
	public int addSystem(Category category) {
		try {
			return categoryMapper.addSystem(category);
		} catch (Exception e) {
			log.error("addSystem error >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 更新类目
	 */
	public int updateSystem(Category category) {
		try {
			return categoryMapper.updateSystem(category);
		} catch (Exception e) {
			log.error("updateSystem error >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 删除类目
	 */
	public int deleteSystem(Category category) {
		try {
			return categoryMapper.deleteSystem(category);
		} catch (Exception e) {
			log.error("deleteSystem error >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 查询系统信息
	 */
	public ResultBean querySystem(String id) {
		try {
			return categoryMapper.querySystem(id);
		} catch (Exception e) {
			log.error("querySystem error,id=" + id + " >>>", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 查询所有系统信息
	 */
	public Response findAllSystem() {
		Response result = new Response();
		List<ResultBean> sysList = categoryMapper.findSystemList();
		List<SysBean> allList = categoryMapper.searchAll();
		List list = new ArrayList();
		for (int i = 0; i < sysList.size(); i++) {
			List list1 = new ArrayList();
			Map map1 = new HashMap();
			ResultBean a = sysList.get(i);
			map1.put(Constants.id, a.getCode());
			map1.put(Constants.name, a.getName());
			map1.put(Constants.sort, a.getSort());
			for (int y = 0; y < allList.size(); y++) {
				Map map2 = new HashMap();
				SysBean b = allList.get(y);
				if (a.getCode().equals(b.getId())) {
					map2.put(Constants.id, b.getCode());
					map2.put(Constants.name, b.getSubSystemName());
					map2.put(Constants.sort, b.getSort());
					list1.add(map2);
				}
			}
			map1.put("subSystem", list1);
			list.add(map1);
		}
		result.setCode(200);
		result.setData(list);
		return result;
	}
}
