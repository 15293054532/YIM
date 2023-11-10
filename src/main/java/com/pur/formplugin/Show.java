package com.pur.formplugin;

import com.alibaba.druid.util.StringUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.workflow.form.operate.flowchart.ViewFlowchartConstant;

import java.util.HashMap;


public class Show extends AbstractBillPlugIn {
    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String key = evt.getItemKey();
        if (StringUtils.equals("tpv_diagram", key)) {//点击按钮
            FormShowParameter showParameter = new FormShowParameter();
            showParameter.setFormId("tpv_my_documentdiag");//动态表单
            DynamicObjectCollection treeData = this.getModel().getEntryEntity("tpv_my_purchasereq_tr");
            showParameter.setClientParam(ViewFlowchartConstant.PROCINSTID, treeData);
            showParameter.getOpenStyle().setShowType(ShowType.Modal);
            HashMap<String, Object> map = new HashMap<>();
            map.put("entity", treeData);
            showParameter.setCustomParams(map);//传参
            this.getView().showForm(showParameter);
        }

    }
}
