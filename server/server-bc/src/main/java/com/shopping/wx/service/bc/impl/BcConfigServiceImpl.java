package com.shopping.wx.service.bc.impl;

import com.alibaba.fastjson.JSONArray;
import com.aliyun.oss.OSSClient;
import com.shopping.base.domain.bc.BcBanner;
import com.shopping.base.domain.bc.BcConfig;
import com.shopping.base.foundation.base.service.impl.BaseServiceImpl;
import com.shopping.base.foundation.constant.AliOssCons;
import com.shopping.base.foundation.dao.bc.BcConfigDAO;
import com.shopping.base.foundation.result.ActionResult;
import com.shopping.base.repository.bc.BcBannerRepository;
import com.shopping.base.repository.bc.BcConfigRepository;
import com.shopping.base.utils.CommUtils;
import com.shopping.framework.oss.AliOSSUtil;
import com.shopping.framework.oss.bean.AliOSSConfig;
import com.shopping.wx.service.bc.BcConfigService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;

@Log4j2
@Service("bcConfigServiceImpl")
@Transactional
public class BcConfigServiceImpl extends BaseServiceImpl<BcConfig,Long> implements BcConfigService {

    @Autowired
    private BcConfigDAO  bcConfigDAO;
    @Autowired
    private BcConfigRepository bcConfigRepository;

    @Autowired
    private BcBannerRepository bcBannerRepository;

    @Override
    public BcConfig getConfigByAppId(String appId) throws Exception {
        BcConfig cfg= this.bcConfigRepository.findByAppId(appId);
        return  cfg;
    }

    @Override
    public ActionResult saveOrUpdate(String appId,BcConfig config,String imgUrlList) throws Exception {
        config.setAppId(appId);
        // 按 appId upsert: 每个 appId 只应有一条配置。用列表读取防止历史脏数据(多行)导致 NonUniqueResult。
        List<BcConfig> existList = this.bcConfigRepository.findAllByAppIdOrderByIdAsc(appId);
        if(existList==null || existList.isEmpty()){
            this.save(config);
        }else{
            config.setId(existList.get(0).getId()); //复用首行 -> 更新
            this.update(config);
            //清理多余的脏数据行
            for(int i=1;i<existList.size();i++){
                this.bcConfigRepository.delete(existList.get(i));
            }
        }
        //保存Banner
        if(CommUtils.isNotNull(imgUrlList)){
            JSONArray arr = JSONArray.parseArray(imgUrlList);
            List<BcBanner>  list = new ArrayList<>();
            for (int i=0;i<arr.size();i++){
                String imgUrl = arr.getString(i);
                BcBanner banner =new BcBanner();
                banner.setAppId(appId);
                banner.setImgUrl(imgUrl);
                banner.setAddTime(new Date());
                list.add(banner);
            }
            if(list!=null && list.size()>0){
                this.bcBannerRepository.saveAll(list);
            }
        }

        return ActionResult.ok();
    }





}
