package com.pur.formplugin;

import com.alibaba.druid.util.StringUtils;
import kd.bos.algo.*;
import kd.bos.algo.input.CollectionInput;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.ArrayList;
import java.util.List;

import static kd.macc.aca.algox.costcalc.action.AbstractActSingleCheckAction.algoKey;

/**
 * @author 84514
 */
public class PurchaseReport extends AbstractReportListDataPlugin {
    @Override
    public DataSet query(ReportQueryParam param, Object o) throws Throwable {
        FilterInfo filter = param.getFilter();
        String billnum = filter.getString("tpv_textfield");// 单据编号
        String combillnum = filter.getString("tpv_textfield1");// 对比单号
        String pricetrantype = filter.getString("tpv_combofield");// 运价表类型

        // 根据过滤条件单据编号 查询运价单
        QFilter QFilter = new QFilter("billno", QCP.equals, billnum);// 查询单据编号相等的
        QFilter.and("billstatus", QCP.equals, "C");// 查询单据状态为已审核的
        String selelctFiles = "tpv_freight.tpv_freight_fl.tpv_basedatafield  tpv_basedatafield,"
                + "tpv_freight.tpv_freight_fl.tpv_basedatafield1 tpv_basedatafield1,"
                + "tpv_freight.tpv_freight_fl.tpv_textfield tpv_textfield2,"
                + "tpv_freight.tpv_freight_fl.tpv_unitfield tpv_unitfield,"
                + "tpv_freight.tpv_freight_fl.tpv_decimalfield tpv_decimalfield,"
                + "tpv_freight.tpv_freight_fl.tpv_decimalfield1 tpv_decimalfield1,"
                + "tpv_freight.tpv_freight_fl.tpv_remark tpv_remark"; // 查询字段
        DataSet dataSet = QueryServiceHelper.queryDataSet(this.getClass().getName(), "tpv_freight", selelctFiles,
                QFilter.toArray(), null);// 根据单据编号查询
        // 查询全部 不包含单据编号字段
        String selelctFile = "tpv_freight.tpv_freight_fl.tpv_basedatafield  tpv_basedatafield,"
                + "tpv_freight.tpv_freight_fl.tpv_basedatafield1 tpv_basedatafield1,"
                + "tpv_freight.tpv_freight_fl.tpv_textfield tpv_textfield2,"
                + "tpv_freight.tpv_freight_fl.tpv_unitfield tpv_unitfield,"
                + "tpv_freight.tpv_freight_fl.tpv_decimalfield tpv_lastpricec,"
                + "tpv_freight.tpv_freight_fl.tpv_decimalfield1 tpv_lastpricez,"
                + "tpv_freight.tpv_freight_fl.tpv_remark tpv_remark"; // 查询字段
        QFilter QFilterss = new QFilter("billno", QCP.not_equals, billnum);// 查询不等于单据编号的
        QFilterss.and("billstatus", QCP.equals, "C");// 查询单据状态为已审核的
        QFilterss.and("tpv_combofield", QCP.equals, pricetrantype);// 查询运价表相同的

        if (!StringUtils.isEmpty(combillnum)) {
            QFilterss.and("billno", QCP.equals, combillnum);
        }
        DataSet data = QueryServiceHelper.queryDataSet(this.getClass().getName(), "tpv_freight", selelctFile,
                QFilterss.toArray(), "tpv_freight.tpv_datefield desc");// 根据单据编号查询

        // reDataSet = new String().add("tpv_comfallrise",null);
        // String tpv_comfallrise = new String("tpv_comfallrise");
        DataSet reDataSet = dataSet.leftJoin(data).on("tpv_basedatafield", "tpv_basedatafield")
                .on("tpv_basedatafield1", "tpv_basedatafield1").on("tpv_textfield2", "tpv_textfield2")
                .on("tpv_unitfield", "tpv_unitfield")
                .select(new String[] { "tpv_basedatafield", "tpv_basedatafield1", "tpv_textfield2", "tpv_unitfield",
                                "tpv_decimalfield", "tpv_decimalfield1", "tpv_remark" },
                        new String[] { "tpv_lastpricec", "tpv_lastpricez" })
                .finish();

        reDataSet = reDataSet.select("tpv_basedatafield", "tpv_basedatafield1", "tpv_textfield2", "tpv_unitfield",
                "tpv_decimalfield", "tpv_decimalfield1", "tpv_remark", "tpv_lastpricec", "tpv_lastpricez");
        // 转换类型
        DynamicObjectCollection col_s = ORM.create().toPlainDynamicObjectCollection(reDataSet);
        List<Field> fields = new ArrayList<>();
        List<Object[]> allrows = new ArrayList<>();
        fields.add(new Field("tpv_basedatafield", DataType.LongType));// 航线起点
        fields.add(new Field("tpv_basedatafield1", DataType.LongType));// 航线终点
        fields.add(new Field("tpv_textfield2", DataType.StringType));// 形式
        fields.add(new Field("tpv_unitfield", DataType.LongType));// 单位
        fields.add(new Field("tpv_decimalfield", DataType.BigDecimalType));// 本期价格从
        fields.add(new Field("tpv_decimalfield1", DataType.BigDecimalType));// 本期价格至
        fields.add(new Field("tpv_lastpricec", DataType.BigDecimalType));// 上期价格从
        fields.add(new Field("tpv_lastpricez", DataType.BigDecimalType));// 上期价格至
        fields.add(new Field("tpv_change", DataType.StringType));// 涨跌
        fields.add(new Field("tpv_textfield3", DataType.StringType));// 较上期涨跌
        fields.add(new Field("tpv_remark", DataType.StringType));// 备注
        int size = fields.size();// DataSet字段数
        if (size > 0) {
            allrows = toallrows(col_s, size);
        }
        // 创建dataset
        CollectionInput collectionInput = new CollectionInput(new RowMeta(fields.toArray(new Field[0])), allrows);
        DataSet createDataSet = Algo.create(algoKey).createDataSet(collectionInput);
        return createDataSet.distinct();
    }

    private List<Object[]> toallrows(DynamicObjectCollection colS, int size) {
        return null;
    }
}
