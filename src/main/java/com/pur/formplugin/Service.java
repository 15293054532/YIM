package com.pur.formplugin;

import com.alibaba.druid.util.StringUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author 84514
 */
public class Service extends AbstractBillPlugIn {
    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        // 判断是否为检查必填项按钮按钮
        if (StringUtils.equals("tpv_confirm", itemKey)) {
            // 获取到采购组织
            DynamicObject billNo = (DynamicObject) this.getView().getModel().getValue("tpv_org");
            // 获取申请部门
            DynamicObject applyOrg = (DynamicObject) this.getView().getModel().getValue("tpv_applyorg");
            // 获取申请人
            DynamicObject applier = (DynamicObject) this.getView().getModel().getValue("tpv_applier");
            // 获取汇率表
            DynamicObject exrateTable = (DynamicObject) this.getView().getModel().getValue("tpv_exratetable");
            // 获取结算币
            DynamicObject tocurr = (DynamicObject) this.getView().getModel().getValue("tpv_tocurr");
            // 获取汇率日期
            Date exratedate = (Date) this.getView().getModel().getValue("tpv_exratedate");
            List<String> list = new ArrayList<>();
            // 判断采购组织是否为空
            if (billNo == null) {
                String billnoString = "必填项采购组织为空";
                list.add(billnoString);
            }
            // 判断申请部门是否为空
            if (applyOrg == null) {
                String applyorgString = "必填项申请部门为空";
                list.add(applyorgString);
            }
            // 判断申请人是否为空
            if (applier == null) {
                String applierString = "必填项申请人为空";
                list.add(applierString);
            }
            // 判断汇率表是否为空
            if (exrateTable == null) {
                String exrateTableString = "必填项汇率表为空";
                list.add(exrateTableString);
            }
            // 判断结算币是否为空
            if (tocurr == null) {
                String tocurrString = "必填项结算币为空";
                list.add(tocurrString);
            }
            // 判断汇率日期是否为空
            if (exratedate == null) {
                String exratedateString = "必填项汇率日期为空";
                list.add(exratedateString);
            }
            String result = String.join(", ", list);
            if (list.isEmpty()) {
                this.getView().showMessage("基本信息必填项已全部填完");
            } else {
                this.getView().showMessage(result);
            }
        }
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        String itemKey = evt.getItemKey();
        // 判断是否为提交按钮
        if (StringUtils.equals("bar_submit", itemKey)) {
            this.submitIncident(evt);
            this.itemClick(evt);
        }

    }

    public void submitIncident(BeforeItemClickEvent evt) {
        // 拿到单据用途字段
        String materiel = (String) this.getView().getModel().getValue("tpv_usage");
        // 判断用途是否为空
        if (materiel.isEmpty()) {
            evt.setCancel(true);
            this.getView().showErrorNotification("提交时用途不可为空");
        }

    }
}
