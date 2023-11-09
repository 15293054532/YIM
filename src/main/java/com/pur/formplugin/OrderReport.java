package com.pur.formplugin;

import java.util.EventObject;
import java.util.List;

import kd.bos.bill.OperationStatus;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowType;
import kd.bos.form.StyleCss;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.field.TextEdit;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.events.CellStyleRule;
import kd.bos.report.plugin.AbstractReportFormPlugin;

/**
 * 运价单运价报表表单插件 点击单据编号和对比单号跳出弹窗 且根据不同的状态改变不同的颜色
 * @author 84514
 */
public class OrderReport extends AbstractReportFormPlugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        //申请单号
        TextEdit bill = this.getView().getControl("tpv_bill");
        //单据状态
        TextEdit billnum = this.getView().getControl("tpv_billstatus");
        bill.addButtonClickListener(this);
        billnum.addButtonClickListener(this);
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        Control c = (Control) evt.getSource();
        String key = c.getKey().toLowerCase();
        if (key.equals("tpv_bill")) {
            bill();
        }
        if (key.equals("tpv_billstatus")) {
            billnum();
        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent e) {
        super.closedCallBack(e);
        String actionId = e.getActionId();
        IDataModel model = this.getModel();
        if ("tpv_textfield".equals(actionId)) {
            ListSelectedRowCollection listData = (ListSelectedRowCollection) e.getReturnData();
            if (listData != null && listData.size() > 0) {
                String BillNo = listData.get(0).getBillNo();
                if (BillNo != null) {
                    model.setValue("tpv_textfield", BillNo);
                }
            }
        }
        if ("tpv_textfield1".equals(actionId)) {
            ListSelectedRowCollection listDatas = (ListSelectedRowCollection) e.getReturnData();
            if (listDatas != null && listDatas.size() > 0) {
                String BillNo = listDatas.get(0).getBillNo();
                if (BillNo != null) {
                    model.setValue("tpv_textfield1", BillNo);
                }
            }
        }
    }

    /**
     * 点击文本按钮弹出列表框架
     */
    public void bill() {
        String tpv_pricetrantype = (String) this.getModel().getValue("tpv_combofield");
        ListShowParameter listpara = new ListShowParameter();
        listpara.setBillFormId("tpv_freight");
        listpara.setParentPageId(this.getView().getPageId());
        listpara.setMultiSelect(false);
        listpara.setStatus(OperationStatus.VIEW);
        listpara.getOpenStyle().setShowType(ShowType.Modal);
        ListFilterParameter listFilterParameter = new ListFilterParameter();
        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C");
        qFilter.and(new QFilter("tpv_combofield", QCP.equals, tpv_pricetrantype));
        listFilterParameter.setFilter(qFilter);
        listpara.setListFilterParameter(listFilterParameter);
        listpara.getOpenStyle().setCacheId(listpara.getPageId());
        listpara.setLookUp(true);
        // 设置弹出子单据页面的样式
        StyleCss inlineStyleCss = new StyleCss();
        inlineStyleCss.setHeight("800");
        inlineStyleCss.setWidth("1400");
        listpara.getOpenStyle().setInlineStyleCss(inlineStyleCss);
        CloseCallBack closeCallBack = new CloseCallBack(this.getClass().getName(), "tpv_textfield");
        listpara.setCloseCallBack(closeCallBack);
        listpara.setCaption("运价单");
        this.getView().showForm(listpara);
    }

    /**

     - 点击文本按钮弹出列表框架
     */
    public void billnum() {
        String tpv_pricetrantype = (String) this.getModel().getValue("tpv_combofield");
        if (tpv_pricetrantype == null) {
            this.getView().showMessage("运价表类型为空，请选择后重试！");
            return;
        }
        ListShowParameter listparas = new ListShowParameter();
        listparas.setBillFormId("tpv_freight");
        listparas.setParentPageId(this.getView().getPageId());
        listparas.setMultiSelect(false);
        listparas.setStatus(OperationStatus.VIEW);
        listparas.getOpenStyle().setShowType(ShowType.Modal);
        ListFilterParameter listFilterParameters = new ListFilterParameter();
        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C");
        qFilter.and(new QFilter("tpv_combofield", QCP.equals, tpv_pricetrantype));
        listFilterParameters.setFilter(qFilter);
        listparas.setListFilterParameter(listFilterParameters);
        listparas.getOpenStyle().setCacheId(listparas.getPageId());
        listparas.setLookUp(true);
        // 设置弹出子单据页面的样式
        StyleCss inlineStyleCss = new StyleCss();
        inlineStyleCss.setHeight("800");
        inlineStyleCss.setWidth("1400");
        listparas.getOpenStyle().setInlineStyleCss(inlineStyleCss);
        CloseCallBack closeCallBack = new CloseCallBack(this.getClass().getName(), "tpv_textfield1");
        listparas.setCloseCallBack(closeCallBack);
        listparas.setCaption("运价单");
        this.getView().showForm(listparas);
    }

    /**

     - 根据值进行赋予颜色
     -
     - @param cellStyleRules
     */
    @Override
    public void setCellStyleRules(List<CellStyleRule> cellStyleRules) {
        CellStyleRule cellStyleRule = new CellStyleRule();
        cellStyleRule.setFieldKey("tpv_textfield3");// 字段标识
        cellStyleRule.setForeColor("#0000FF");// 前景色
        cellStyleRule.setBackgroundColor("#FFFFFF");// 背景色
        cellStyleRule.setDegree(100);// 透明度
        cellStyleRule.setCondition("tpv_change = '平'");// 前置条件，值与表达式计算器一致
        cellStyleRules.add(cellStyleRule);
        CellStyleRule cellStyleRule1 = new CellStyleRule();
        cellStyleRule1.setFieldKey("tpv_textfield3");// 字段标识
        cellStyleRule1.setForeColor("#DC143C");// 前景色
        cellStyleRule1.setBackgroundColor("#FFFFFF");// 背景色
        cellStyleRule1.setDegree(100);// 透明度
        cellStyleRule1.setCondition("tpv_change = '涨'");// 前置条件，值与表达式计算器一致
        cellStyleRules.add(cellStyleRule1);
        CellStyleRule cellStyleRule2 = new CellStyleRule();
        cellStyleRule2.setFieldKey("tpv_textfield3");// 字段标识
        cellStyleRule2.setForeColor("#008000");// 前景色
        cellStyleRule2.setBackgroundColor("#FFFFFF");// 背景色
        cellStyleRule2.setDegree(100);// 透明度
        cellStyleRule2.setCondition("tpv_change = '跌'");// 前置条件，值与表达式计算器一致
        cellStyleRules.add(cellStyleRule2);
        super.setCellStyleRules(cellStyleRules);
    }

    @Override
    public void afterQuery(ReportQueryParam queryParam) {
        super.afterQuery(queryParam);

    }
}