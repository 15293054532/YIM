package com.pur.formplugin;

import com.alibaba.druid.util.StringUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.CustomEventArgs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *采购申请单附件回填功能实现 采购申请单界面插件， 打开动态表单
 * @author 84514
 */
public class Attachments extends AbstractBillPlugIn {
    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String key = evt.getItemKey();
        // 判断是否为回填按钮
        if (StringUtils.equals("tpv_backbarite", key)) {
            // 创建弹出动态表单页面对象
            FormShowParameter showParameter = new FormShowParameter();
            // 设置弹出页面的标识
            showParameter.setFormId("tpv_my_attachment");
            Map<String, Object> map = this.showInfoForm();
            // 存入获取到的动态表单数据
            showParameter.setCustomParams(map);
            // 状态
            showParameter.setStatus(OperationStatus.ADDNEW);
            // 设置页面关闭回调方法
            // CloseCallBack参数：回调插件，回调标识
            showParameter.setCloseCallBack(new CloseCallBack(this, "tpv_backbarite"));
            // 设置弹出页面打开方式
            showParameter.getOpenStyle().setShowType(ShowType.Modal);
            // 打开动态表单
            this.getView().showForm(showParameter);
        }
    }

    /**
     * 动表单获取原单据字段
     */
    private Map<String, Object> showInfoForm() {
        // 获取单据体
        DynamicObject dataEntity = this.getModel().getDataEntity(true);
        // 创建集合保存需要获取的字段值
        Map<String, Object> map = new HashMap<>();
        // 获取申请人
        DynamicObject applier = dataEntity.getDynamicObject("tpv_applier");
        // 获取申请部门编码
        DynamicObject applyOrg = dataEntity.getDynamicObject("tpv_applyorg");
        // 获取用途数据
        String usage = dataEntity.getString("tpv_usage");
        // 获取供应商
//        DynamicObject basedatafield = dataEntity.getDynamicObject("tpv_supplier");
        // 获取币别
//        DynamicObject currency = dataEntity.getDynamicObject("tpv_currency");
        // 判断获得的各项数据
        if (applier != null) {
            map.put("tpv_applier", applier.getLong("id"));
        }
        if (applyOrg != null) {
            map.put("tpv_applyorg", applyOrg.getLong("id"));
        }
//        if (basedatafield != null) {
//            map.put("tpv_supplier", basedatafield.getLong("id"));
//        }
//        if (currency != null) {
//            map.put("tpv_currency", currency.getLong("id"));
//        }
        if (usage != null) {
            map.put("tpv_usage", usage);
        }
        // 附件获取
        // 获取申请单号
        String billno = dataEntity.getString("billno");
        map.put("billno",billno);
        // 获取附件字段的值
        DynamicObjectCollection sourceAttachcol = (DynamicObjectCollection) this.getView().getModel()
                .getValue("tpv_attachment");
        if (sourceAttachcol.size() == 0) {
            return map;
        }
        // 获取源附件字段附件对象id集合
        List<Long> attchIdSet = new ArrayList<>();
        sourceAttachcol.forEach(attach -> {
            attchIdSet.add(attach.getDynamicObject("fbasedataId").getLong("id"));
        });
        // 判断附件数据是否为空
        if (!attchIdSet.isEmpty()) {
            map.put("tpv_attachment", attchIdSet);
        }
        return map;

    }

    /**
     * 动态表单回填单据字段
     */
    private void setInwardInfo(Map<String, Object> values) {
        IDataModel model = this.getModel();
        DynamicObject dataEntity = this.getModel().getDataEntity(true);
        //回填申请编号
        model.setValue("billno",values.get("tpv_billno"));
        // 回填动态表单的申请人
        model.setValue("tpv_applier", values.get("tpv_user"));
        // 回填动态表单的申请部门
        model.setValue("tpv_applyorg", values.get("tpv_applyorg"));
        // 回填动态表单的币种
//        model.setValue("tpv_currency", values.get("tpv_currency"));
//        //回填动态表单的供应商
//        model.setValue("tpv_supplier", values.get("tpv_supplier"));
        // 回填动态表单的用途
        model.setValue("tpv_usage", values.get("tpv_usage"));
        // 回填附件字段
        List<Long> attcheIdSet = (List<Long>) values.get("tpv_attachment");
        if (attcheIdSet != null && !attcheIdSet.isEmpty()) {
            // 回填
            model.setValue("tpv_attachment", attcheIdSet.toArray());
        } else {
            model.setValue("tpv_attachment", null);
        }
        this.getView().invokeOperation("save");
    }

    /**
     * 动态表单关闭回调事件
     */

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        Object returnData = closedCallBackEvent.getReturnData();
        // 判断标识是否匹配，并验证返回值不为空，不验证返回值可能会报空指针
        if (StringUtils.equals(closedCallBackEvent.getActionId(), "tpv_backbarite")
                && null != closedCallBackEvent.getReturnData()) {
            // 这里返回对象为Object，可强转成相应的其他类型，
            // 单条数据可用String类型传输，返回多条数据可放入map中，也可使用json等方式传输
            HashMap<String, Object> values = (HashMap<String, Object>) returnData;
            this.setInwardInfo(values);
        }
    }
}

