package com.pijiuji.admin.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pijiuji.admin.service.PjjService;
import com.pijiuji.bean.*;
import com.pijiuji.mapper.*;
import com.pijiuji.util.MapTrunBean;
import com.pijiuji.util.Param;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PjjServiceImpl implements PjjService {
    @Autowired
    private PjjMapper pjjMapper;

    @Autowired
    private LevelUserMapper levelUserMapper;

    @Autowired
    private ShopSpecificationMapper shopSpecificationMapper;

    @Autowired
    private SpecificationMapper specificationMapper;
    
    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public ResponseResult findAdminAllPjj(HttpServletRequest request) {
        String page = request.getParameter("page");
        PageHelper.startPage(Integer.valueOf(page), Param.pageSize);
        String userId = request.getParameter("userId");
        if (StringUtils.isEmpty(userId)) {
            return new ResponseResult(500,"还没登录");
        }
        PjjExample pjjExample = new PjjExample();
        PjjExample.Criteria criteria = pjjExample.createCriteria();
        List<Map> resultList = new ArrayList<>();

        LevelUser levelUser = levelUserMapper.selectByPrimaryKey(userId);
        Integer levelRoleId = levelUser.getLevelRoleId();
        SysRole sysRole = sysRoleMapper.selectByPrimaryKey(levelRoleId);
        String name = sysRole.getName();
        if ("admin".equalsIgnoreCase(name)) {
            List<Pjj> pjjs = pjjMapper.selectByExample(pjjExample);

            for (int i = 0; i < pjjs.size(); i++) {
                String pjjUserId = pjjs.get(i).getPjjUserId();
                if (StringUtils.isEmpty(pjjUserId)) {
                    pjjs.get(i).setPjjUserId("未分配");
                }else {
                    LevelUser levelUser1 = levelUserMapper.selectByPrimaryKey(pjjUserId);
                    pjjs.get(i).setPjjUserId(levelUser1.getLevelUserName());
                }
                Map<String, Object> pjjMap = MapTrunBean.beanToMap(pjjs.get(i));
                resultList.add(pjjMap);
            }

        }else {
            //不是超级管理员
            criteria.andPjjUserIdEqualTo(userId);
            List<Pjj> pjjs = pjjMapper.selectByExample(pjjExample);
            for (Pjj pjj : pjjs) {
                pjj.setPjjUserId(levelUser.getLevelUserName());
                Map<String, Object> pjjMap = MapTrunBean.beanToMap(pjj);
                resultList.add(pjjMap);
            }
        }

        for (int i = 0; i < resultList.size(); i++) {
            String pjjStatus = resultList.get(i).get("pjjStatus").toString();
            if ("0".equals(pjjStatus)) {
                resultList.get(i).put("pjjToStatus","异常");
            }else if ("1".equals(pjjStatus)) {
                resultList.get(i).put("pjjToStatus","正常");
            }else {
                resultList.get(i).put("pjjToStatus","回收");
            }
        }
        PageInfo pageInfo = new PageInfo(resultList);
        return new ResponseResult(200,"查询成功",pageInfo);
    }

    @Override
    public ResponseResult savePjj(HttpServletRequest request) {
        Pjj pjj = new Pjj();
        String pjjCode = request.getParameter("pjjCode");
        pjj.setPjjCode(pjjCode);
        int i = pjjMapper.insertSelective(pjj);
        //查询出来所有规格
        SpecificationExample specificationExample = new SpecificationExample();
        specificationExample.createCriteria();
        List<Specification> specifications = specificationMapper.selectByExample(specificationExample);
        ShopSpecification shopSpecification = new ShopSpecification();
        int i1 = 0;
        for (Specification specification : specifications) {
            shopSpecification.setPjjId(pjj.getPjjId());
            shopSpecification.setSpecId(specification.getSpeId());
            i1+= shopSpecificationMapper.insertSelective(shopSpecification);
        }
        if (i > 0 &&i1 >= specifications.size()) {
            return new ResponseResult(200,"修改成功");
        }

        return new ResponseResult(500,"修改失败");
    }

    @Override
    public ResponseResult updatePjj(HttpServletRequest request) {
        String pjjIds = request.getParameter("pjjIds");
        String userId = request.getParameter("userId");
        Pjj pjj = new Pjj();
        if (StringUtils.isEmpty(pjjIds)) {
            return new ResponseResult(500,"啤酒机id集合不能为空");
        }
        if (StringUtils.isEmpty(userId)) {
            return new ResponseResult(500,"分配的用户id不能为空");
        }
        String[] split = pjjIds.split(",");
        for (String pjjId : split) {
            pjj.setPjjUserId(userId);
            pjj.setPjjId(Integer.valueOf(pjjId));
            pjjMapper.updateByPrimaryKeySelective(pjj);
        }
        return new ResponseResult(200,"分配成功");
    }
}