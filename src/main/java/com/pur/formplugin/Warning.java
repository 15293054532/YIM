package com.pur.formplugin;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.datamodel.AbstractFormDataModel;
import kd.bos.entity.datamodel.TableValueSetter;
import kd.bos.form.control.Control;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.fi.bcm.fel.common.ArrayUtils;
import kd.tmc.fbp.common.enums.BillStatusEnum;
import kd.tmc.fbp.common.helper.TmcDataServiceHelper;
import kd.tmc.fbp.common.util.DateUtils;
import kd.tmc.fbp.formplugin.edit.AbstractTmcBillEdit;

import java.util.Date;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author 84514
 */
public class Warning extends AbstractTmcBillEdit {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 监听按钮
        this.addClickListeners("tpv_freshbutton");//查询
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        long curOrgId = RequestContext.get().getOrgId();
        QFilter filter = new QFilter("tpv_org", "=", curOrgId);//组织
        DynamicObject initOrg = QueryServiceHelper.queryOne("tpv_my_purchasereq", "id,tpv_exratetable,tpv_currency", filter.toArray());
        if (initOrg != null) {
            this.getModel().setValue("tpv_ratetable", initOrg.get("tpv_exratetable"));
            this.getModel().setValue("tpv_reportcurrency", initOrg.get("tpv_currency"));
        }
        this.showData();
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control) evt.getSource();
        String key = source.getKey().toLowerCase();
        if ("tpv_freshbutton".equals(key)) {
            this.showData();
        }
    }

    private void showData() {
        DynamicObject[] payableBills = this.getDate();
        AbstractFormDataModel model = (AbstractFormDataModel) this.getModel();
        model.deleteEntryData("tpv_entryentity");//卡片单据体
        DynamicObject rateTable = (DynamicObject) this.getModel().getValue("tpv_ratetable");//卡片汇率表
        DynamicObject reportCurrency = (DynamicObject) this.getModel().getValue("tpv_reportcurrency");//卡片结算币别
        if (rateTable == null) {
            this.getView().showTipNotification(ResManager.loadKDString("汇率表不能为空。", "ExpiredWarnPlugin_1", "tmc-cdm-formplugin"));//插件基类
        } else if (reportCurrency == null) {
            this.getView().showTipNotification(ResManager.loadKDString("报告币别不能为空。", "ExpiredWarnPlugin_2", "tmc-cdm-formplugin"));
        } else if (!ArrayUtils.isEmpty(payableBills)) {
            model.beginInit();
            TableValueSetter vs = new TableValueSetter();
            List<Long> sourceCurrencys = Arrays.stream(payableBills)
                    .filter(v -> v.getDynamicObject("tpv_currency") != null)
                    .map(v -> v.getDynamicObject("tpv_currency").getLong("id"))
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, BigDecimal> sourceCurrencyMap = new HashMap<>(10);

            Long sourceCurrency;
            BigDecimal value;
            for (Iterator<Long> iterator = sourceCurrencys.iterator(); iterator.hasNext(); ) {
                sourceCurrency = iterator.next();
                value = BaseDataServiceHelper.getExchangeRate(rateTable.getLong("id"), sourceCurrency, reportCurrency.getLong("id"), new Date());
                if (value == null) {
                    value = BigDecimal.ZERO;
                }
                sourceCurrencyMap.put(sourceCurrency, value);
            }
            vs.addField("tpv_org");//采购组织
            vs.addField("tpv_type");//采购类型
            vs.addField("tpv_use");//用途
            vs.addField("tpv_billstatus");//单据状态
            vs.addField("tpv_contract");//合同号
            vs.addField("tpv_appdate");//申请日期
            vs.addField("tpv_stacurrency");//本位币
            vs.addField("tpv_setcurrency");//结算金
            vs.addField("tpv_maturity");//单据到期
            vs.addField("tpv_user");//申请人
            vs.addField("tpv_full");//供应商
            vs.addField("tpv_auditor");//审核人
            for (DynamicObject bill : payableBills) {
                vs.addRow(
                        bill.get("tpv_org.id"),
                        bill.get("tpv_billtypefield.name"),
                        bill.get("tpv_usage"),
                        bill.get("billstatus"),
                        bill.get("tpv_contract"),
                        bill.get("tpv_applydate"),
                        bill.get("tpv_currency.id"),
                        bill.get("tpv_amoun"),
                        bill.get("tpv_duedate"),
                        bill.get("tpv_applier.name"),
                        bill.get("tpv_supplier.name"),
                        bill.get("auditor.name")
                );
            }
            model.batchCreateNewEntryRow("tpv_entryentity", vs);
            model.endInit();
            this.getView().updateView("tpv_entryentity");
        }
    }

    private DynamicObject[] getDate() {
        int day = (int) this.getModel().getValue("day");
        if (day == 0) {
            this.getView().showTipNotification(ResManager.loadKDString("请输入到期天数。", "ExpiredWarnPlugin_0", "tmc-cdm-formplugin"));
            return null;
        } else {
            QFilter filters = new QFilter("tpv_duedate", "<=", DateUtils.getNextDay(new Date(), day));//单据到期
            QFilter statusFilters = new QFilter("billstatus", "=", BillStatusEnum.AUDIT.getValue());//状态
            DynamicObject[] bills = TmcDataServiceHelper.load("tpv_my_purchasereq", getFields(), new QFilter[]{filters, statusFilters}, "tpv_duedate");
            List<DynamicObject> allBills = new ArrayList<>(Arrays.asList(bills));
            return allBills.toArray(new DynamicObject[0]);
        }
    }

    private String getFields() {
        return "tpv_org,tpv_billtypefield,tpv_usage,billstatus,tpv_contract,tpv_applydate,tpv_currency,tpv_amoun,tpv_duedate,tpv_applier,tpv_supplier,tpv_duedate,auditor,tpv_user";
    }
}
