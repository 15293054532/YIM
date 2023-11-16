package com.pur.card;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.ShowType;
import kd.bos.form.chart.Axis;
import kd.bos.form.chart.AxisType;
import kd.bos.form.chart.BarSeries;
import kd.bos.form.chart.Chart;
import kd.bos.form.chart.Label;
import kd.bos.form.chart.Position;
import kd.bos.form.chart.XAlign;
import kd.bos.form.chart.YAlign;
import kd.bos.form.control.Control;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QFilter;
import kd.bos.report.ReportShowParameter;
import kd.tmc.fbp.common.enums.BillStatusEnum;
import kd.tmc.fbp.common.enums.CreditFinTypeEnum;
import kd.tmc.fbp.common.helper.TmcDataServiceHelper;
import kd.tmc.fbp.common.util.EmptyUtil;

/**
 * @author 84514
 */
public class CardLine extends AbstractFormPlugin implements BeforeF7SelectListener {

    public CardLine() {
    }

    private Set<String> getSelectProps() {
        Set<String> props = new HashSet(16);
        props.add("tpv_org");//组织
        props.add("billno");//申请单号
        props.add("tpv_currency");//本位币
        props.add("tpv_my_purchasereq_sp.tpv_applyqty");//申请数量
        props.add("tpv_my_purchasereq_sp.tpv_orderedqty");//已订货数量
        props.add("tpv_supplier");//供应商
        return props;
    }

    /**
     * 控件监听
     */
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit companyF7 = this.getControl("tpv_company");
        companyF7.addBeforeF7SelectListener(this);
        BasedataEdit bankF7 = this.getControl("tpv_supplier");
        bankF7.addBeforeF7SelectListener(this);
        this.addClickListeners("tpv_chartap", "tpv_refresh");
    }

    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        this.updateCreditChart();//更新图表
    }

    /**
     * 属性变化
     */
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        switch (e.getProperty().getName()) {
            case "tpv_company":
                this.updateCreditChart();
                break;
            case "tpv_stacurrency":
                this.updateCreditChart();
            case "tpv_currencyunit":
            case "tpv_supplier":
                this.updateCreditChart();
        }

    }

    /**
     * 点击控件
     */
    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control) evt.getSource();
        if ("tpv_chartap".equals(source.getKey())) {
            if (!this.verifyQuery()) {
                return;
            }

            IDataModel model = this.getModel(); //当前数据模型
            ReportShowParameter showParameter = new ReportShowParameter(); //设置报表显示参数
            showParameter.setFormId("tpv_my_purrep");//采购报表
            showParameter.getOpenStyle().setShowType(ShowType.Modal);

            ReportQueryParam queryParam = new ReportQueryParam();//设置报表查询参数
            FilterInfo filter = new FilterInfo();
            filter.addFilterItem("tpv_billstatus", CreditFinTypeEnum.FINORG.getValue());
            queryParam.setFilter(filter);
            showParameter.setQueryParam(queryParam);
            this.getView().showForm(showParameter);//显示报表页面
        } else if ("tpv_refresh".equals(source.getKey()) && this.updateCreditChart()) {
            this.getView().showSuccessNotification(ResManager.loadKDString("刷新成功", "CreditCardPlugin_10", "tmc-creditm-formplugin"));
        }

    }

    private Boolean updateCreditChart() {//更新图表
        if (!this.verifyQuery()) {
            return Boolean.FALSE;
        } else {
            List<String> xDimensions = new ArrayList();//图标x轴
            List<BigDecimal[]> charDataList = new ArrayList();//存储图表数据
            Boolean isExitData = this.getCharData(xDimensions, charDataList);
            if (!isExitData) {
                this.getDefaultData(xDimensions, charDataList);
            }

            Chart customchart = this.getControl("tpv_chartap");//获取图表
            customchart.clearData();
            customchart.setMargin(Position.top, "35px");
            customchart.setMargin(Position.left, "5%");
            customchart.setMargin(Position.bottom, "35px");
            customchart.setLegendAlign(XAlign.center, YAlign.bottom);
            customchart.setShowTooltip(true);
            this.setCreditchartXaxisTick(customchart, xDimensions);
            this.setCreditChartData(customchart, charDataList);//图表数据
            return isExitData;
        }
    }

    /**
     * 数据获取
     */
    private boolean getCharData(List<String> xDimensions, List<BigDecimal[]> charDataList) {
        Long divideCount = this.getCurrencyUnit();
        QFilter companyFilter = this.getOrgFilter();
        QFilter supplierFilter = this.getSupplierFilter();
        QFilter qFilter = this.getcurrencyFilter();
        QFilter sum = (new QFilter("tpv_my_purchasereq_sp.tpv_applyqty", ">", 0));
        QFilter statusFilter = (new QFilter("billstatus", "=", BillStatusEnum.AUDIT.getValue()));
        DynamicObject[] creditLimits = TmcDataServiceHelper.load("tpv_my_purchasereq", String.join(",", this.getSelectProps()), new QFilter[]{statusFilter, sum, companyFilter, supplierFilter, qFilter}, "billno desc");
        if (EmptyUtil.isEmpty(creditLimits)) {
            return Boolean.FALSE;
        } else {
            int count = 0;
            for (DynamicObject creditLimit : creditLimits) {
                if (count < 6) {
                    String billno = (String) creditLimit.get("billno");
                    xDimensions.add(billno);

                    List<DynamicObject> billEntries = (List<DynamicObject>) creditLimit.get("tpv_my_purchasereq_sp");
                    for (DynamicObject entry : billEntries) {

                        BigDecimal tpvApplyqty = (BigDecimal) entry.get("tpv_applyqty");
                        BigDecimal tpvOrderedqty = (BigDecimal) entry.get("tpv_orderedqty");

                        BigDecimal applyqty = tpvApplyqty.divide(new BigDecimal(divideCount));
                        BigDecimal orderedqty = tpvOrderedqty.divide(new BigDecimal(divideCount));


                        BigDecimal[] data = new BigDecimal[]{applyqty, orderedqty};
                        charDataList.add(data);
                    }
                    count++;
                } else {
                    break;
                }
            }
            return Boolean.TRUE;
        }
    }

    private QFilter getSupplierFilter() {//供应商过滤条件
        DynamicObject banks = (DynamicObject) this.getModel().getValue("tpv_supplier");
        QFilter ofilter = null;
        if (banks != null) {
            ofilter = new QFilter("tpv_supplier", "=", banks.getPkValue());
        }
        return ofilter;
    }

    private QFilter getOrgFilter() {//组织过滤条件
        DynamicObject company = (DynamicObject) this.getModel().getValue("tpv_company");
        QFilter ofilter = null;
        if (company != null) {
            ofilter = new QFilter("tpv_org", "=", company.getPkValue());
        }
        return ofilter;
    }

    private QFilter getcurrencyFilter() {//币别过滤条件
        DynamicObject company = (DynamicObject) this.getModel().getValue("tpv_stacurrency");
        QFilter ofilter = null;
        if (company != null) {
            ofilter = new QFilter("tpv_currency", "=", company.getPkValue());
        }
        return ofilter;
    }

    /**
     * 设置图表的X轴刻度
     */
    private Axis setCreditchartXaxisTick(Chart customchart, List<String> xDimensions) {
        Axis xaxis = customchart.createXAxis(null, AxisType.category);
        Map<String, Object> axisTick = new HashMap();
        axisTick.put("interval", 0);
        axisTick.put("show", false);
        xaxis.setPropValue("axisTick", axisTick);
        xaxis.setCategorys(xDimensions);
        this.setLineColor(xaxis, "#666666");
        return xaxis;
    }

    /**
     * 设置图表的数据展示
     */
    private void setCreditChartData(Chart customchart, List<BigDecimal[]> charDataList) {
        BigDecimal[] dataSum = new BigDecimal[charDataList.size()];
        BigDecimal[] useData = new BigDecimal[charDataList.size()];
        for (int i = 0; i < charDataList.size(); i++) {
            BigDecimal[] arr = charDataList.get(i);
            if (arr.length >= 2) {
                dataSum[i] = arr[0];
                useData[i] = arr[1];
            } else {
                dataSum[i] = arr[0];
                useData[i] = arr[0];
            }
        }
        Axis taskNumberAxis = customchart.createYAxis(ResManager.loadKDString("订单数量", "CreditCardPlugin_0", "tmc-creditm-formplugin"), AxisType.value);
        BarSeries seriesNormal = customchart.createBarSeries(ResManager.loadKDString("申请数量", "CreditCardPlugin_2", "tmc-creditm-formplugin"));
        seriesNormal.setBarWidth("20%");
        seriesNormal.setColor("#00CCCC");
        seriesNormal.setData(dataSum);
        Label lab = new Label();
        lab.setShow(false);
        seriesNormal.setLabel(lab);
        BarSeries seriesOverdue = customchart.createBarSeries(ResManager.loadKDString("已订货数量", "CreditCardPlugin_3", "tmc-creditm-formplugin"));
        seriesOverdue.setBarWidth("20%");
        seriesOverdue.setColor("#098BFF");
        seriesOverdue.setData(useData);
        seriesOverdue.setLabel(lab);
        List<Object> path = new ArrayList<>();
        path.add("label");
        path.add("normal");
        path.add("formatter");
        seriesOverdue.addFuncPath(path);
        if (EmptyUtil.isNoEmpty(dataSum)) {
            BigDecimal maxData = this.getMaxAmt(dataSum);
            long maxTask = maxData.setScale(0, 4).longValue();
            int baseParam = this.getBaseParam(maxTask);
            long yMaxValue = maxTask != 0L && maxTask % (long) baseParam == 0L ? maxTask : (Math.floorDiv(maxTask, baseParam) + 1L) * (long) baseParam;
            long interval = yMaxValue / 5L;
            taskNumberAxis.setMax(yMaxValue);
            taskNumberAxis.setInterval(interval);
        }
        Map<String, Object> axisTick = new HashMap<>();
        axisTick.put("interval", 0);
        axisTick.put("show", false);
        taskNumberAxis.setPropValue("axisTick", axisTick);
        this.setLineColor(taskNumberAxis, "#666666");
        customchart.bindData(null);
    }

    /**
     * 找到最大值
     */
    private BigDecimal getMaxAmt(BigDecimal[] data) {
        BigDecimal maxAmt = BigDecimal.ZERO;
        List<BigDecimal> sumDatas = (List) Arrays.stream(data).filter((o) -> {
            return o != null;
        }).collect(Collectors.toList());
        if (EmptyUtil.isNoEmpty(sumDatas)) {
            Iterator<BigDecimal> ites = sumDatas.iterator();
            maxAmt = ites.next();

            while (ites.hasNext()) {
                BigDecimal amt = ites.next();
                if (amt != null && maxAmt.compareTo(amt) < 0) {
                    maxAmt = amt;
                }
            }
        }
        return maxAmt;
    }

    /**
     * 根据给定的最大任务值maxTask，返回相应的基础参数
     */
    private int getBaseParam(long maxTask) {
        int baseParam;
        if (maxTask <= 100L) {
            baseParam = 25;
        } else if (maxTask <= 500L) {
            baseParam = '2';
        } else if (maxTask <= 1000L) {
            baseParam = 'd';
        } else if (maxTask <= 5000L) {
            baseParam = 500;
        } else if (maxTask <= 10000L) {
            baseParam = 1000;
        } else if (maxTask <= 50000L) {
            baseParam = 5000;
        } else if (maxTask <= 100000L) {
            baseParam = 10000;
        } else {
            baseParam = '썐';
        }
        return baseParam;
    }

    /**
     * 添加默认数据
     */
    private void getDefaultData(List<String> xDimensions, List<BigDecimal[]> charDataList) {
        xDimensions.add(" ");
        charDataList.add(new BigDecimal[]{BigDecimal.ZERO});
        charDataList.add(new BigDecimal[]{BigDecimal.ZERO});
        charDataList.add(new BigDecimal[]{BigDecimal.ZERO});
    }

    /**
     * 给定的轴设置线的颜色属性
     */
    private void setLineColor(Axis axix, String color) {
        Map<String, Object> axisLineMap = new HashMap(1);
        Map<String, Object> lineStyleMap = new HashMap(1);
        lineStyleMap.put("color", color);
        axisLineMap.put("lineStyle", lineStyleMap);
        axix.setPropValue("axisLine", axisLineMap);
    }

    /**
     * 验证方法，验证分析图参数是否完整填写
     */
    private Boolean verifyQuery() {
        DynamicObject companys = (DynamicObject) this.getModel().getValue("tpv_company");
        if (EmptyUtil.isEmpty(companys)) {
            this.getView().showTipNotification(ResManager.loadKDString("请选择采购组织", "CreditCardPlugin_7", "tmc-creditm-formplugin"));
            return Boolean.FALSE;
        } else {
            DynamicObject currency = (DynamicObject) this.getModel().getValue("tpv_stacurrency");
            if (EmptyUtil.isEmpty(currency)) {
                this.getView().showTipNotification(ResManager.loadKDString("请选择币别", "CreditCardPlugin_8", "tmc-creditm-formplugin"));
                return Boolean.FALSE;
            } else {
                String currencyUnit = (String) this.getModel().getValue("tpv_currencyunit");
                if (StringUtils.isBlank(currencyUnit)) {
                    this.getView().showTipNotification(ResManager.loadKDString("请选择数量单位", "CreditCardPlugin_9", "tmc-creditm-formplugin"));
                    return Boolean.FALSE;
                } else {
                    return Boolean.TRUE;
                }
            }
        }
    }

    //数量单位获取对应的数值
    private Long getCurrencyUnit() {
        String currencyUnit = (String) this.getModel().getValue("tpv_currencyunit");
        Long divideCount = 1L;
        if ("Billion".equals(currencyUnit)) {
            divideCount = 100000000L;
        } else if ("TenThousand".equals(currencyUnit)) {
            divideCount = 10000L;
        } else if ("Million".equals(currencyUnit)) {
            divideCount = 1000000L;
        } else if ("Thousand".equals(currencyUnit)) {
            divideCount = 1000L;
        } else if ("Original".equals(currencyUnit)) {
            divideCount = 1L;
        }
        return divideCount;
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
    }
}

