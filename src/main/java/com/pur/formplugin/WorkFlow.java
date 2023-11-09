package com.pur.formplugin;


import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.form.control.Control;
import kd.bos.workflow.design.plugin.IWorkflowDesigner;

import java.util.*;

public class WorkFlow extends AbstractBillPlugIn implements IWorkflowDesigner {
    @Override
    public Map<String, Object> getDesignerInitData(Map<String, Object> map) {

        // 获取树形单据体数据
        JSONArray treeEntity = this.getView().getFormShowParameter().getCustomParam("entity");
        List<RelationTool> nodeList = entityToNodeList(treeEntity);

        List<RelationTool> nodeModels = calcPosition(nodeList);
        String genXml = convertNodeToXml(nodeModels);
        map.put("graph_xml", genXml);
        return map;
    }


    /**
     * 转换
     */
    public List<RelationTool> entityToNodeList(JSONArray jsonArray) {
        List<RelationTool> nodeList = new ArrayList<>();
        createFlowNodeModels(jsonArray, "0", nodeList);
        System.out.println("JSONObject.toJSONString(nodeList) = " + JSONObject.toJSONString(nodeList));
        for (RelationTool node : nodeList) {
            for (Object entity : jsonArray) {
                JSONObject object = (JSONObject) entity;
                if (object.getString("id").equals(node.getNodeId())) {
//                    if (object.containsKey("tpv_my_purchasereq_tr")) {
//                        JSONObject userObject = object.getObject("tpv_my_purchasereq_tr", JSONObject.class);
//                        if (userObject.containsKey("tpv_product")) {
//                            JSONObject nameObject = userObject.getObject("tpv_product", JSONObject.class);
////                            if (nameObject.containsKey("zh_CN")) {
////                                node.setTitle(nameObject.getString("zh_CN"));
////                            } else {
////                                node.setTitle("user name not found");
////                            }
//                        } else {
//                            node.setTitle("user name not found");
//                        }
//                    }
//                    else {
//                        node.setTitle("user name not found");
//                    }
                    if (object.containsKey("tpv_product")) {
                        node.setTitle(object.get("tpv_product").toString());
                    } else {
                        node.setTitle("高新刚");
                    }
                    if (object.containsKey("tpv_integer")) {
                        node.setSubTitle(object.get("tpv_integer").toString());
                    } else {
                        node.setSubTitle("data not found");
                    }
                    if (object.containsKey("tpv_unitprice")) {
                        node.setInfo1(object.get("tpv_unitprice").toString());
                    } else {
                        node.setInfo1("null");
                    }
                    if (object.containsKey("null")) {
                        node.setInfo2(object.get("null").toString());
                    } else {
                        node.setInfo2("null");
                    }
                    if (object.containsKey("seq")) {
                        node.setInfo3(object.get("seq").toString());
                    } else {
                        node.setInfo3("null");
                    }
                }
            }
        }
        return nodeList;
    }

    /**
     * 递归创建流程节点
     */
    private void createFlowNodeModels(JSONArray entity, String parentId, List<RelationTool> nodeList) {
        for (int i = 0; i < entity.size(); i++) {
            JSONObject jsonObject = entity.getJSONObject(i);
            String id = jsonObject.getString("id");
            String pid = "";
            if (jsonObject.get("pid") != null) {
                pid = jsonObject.getString("pid");
            } else {
                pid = "0";
            }
            if (pid.equals(String.valueOf(parentId))) {
                //判断该节点是否为父节点的子节点
                RelationTool node = new RelationTool();
                node.setNodeId(id);
                if (hasChildNodes(entity, id)) {
                    //判断该节点是否有子节点
                    node.setSourceNodeId(id);
                    node.setTargetNodeId(getChildNodeIds(entity, id));
                }
                // 计算层级
                int level = calculateLevel(entity, id);
                node.setLevel(level);
                nodeList.add(node);
                //递归，处理子节点
                createFlowNodeModels(entity, id, nodeList);
            }
        }
    }

    /**
     * 判断子节点存在
     */
    private boolean hasChildNodes(JSONArray entity, String id) {
        for (int i = 0; i < entity.size(); i++) {
            JSONObject jsonObject = entity.getJSONObject(i);
            String pid = "";
            if (jsonObject.get("pid") != null) {
                pid = jsonObject.getString("pid");
            } else {
                pid = "0";
            }
            if (pid.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算子节点
     */
    private List<String> getChildNodeIds(JSONArray entity, String id) {
        List<String> childIds = new ArrayList<>();
        for (int i = 0; i < entity.size(); i++) {
            JSONObject jsonObject = entity.getJSONObject(i);
            String pid = "";
            if (jsonObject.get("pid") != null) {
                pid = jsonObject.getString("pid");
            } else {
                pid = "0";
            }
            if (pid.equals(id)) {
                String childId = jsonObject.getString("id");
                childIds.add(childId);
            }
        }
        return childIds;
    }

    /**
     * 计算节点的层级
     */
    private int calculateLevel(JSONArray entity, String id) {
        List<String> parent = new ArrayList<>();
        //记录层级
        int level = 0;
        //父节点
        String parentId = id;
        while (!parentId.equals("0")) {
            for (int i = 0; i < entity.size(); i++) {
                JSONObject jsonObject = entity.getJSONObject(i);
                String idValue = jsonObject.getString("id");
                String pid = "";
                if (jsonObject.get("pid") != null) {
                    pid = jsonObject.getString("pid");
                    parent.add(pid);
                } else {
                    pid = "0";
                }
                if (idValue.equals(parentId)) {
                    parentId = pid;
                    level++;
                    break;
                }
            }
        }
        System.out.println(parent);
        return level;
    }


    private List<RelationTool> calcPosition(List<RelationTool> nodelList) {
//        int currentX = 0;  // 设为左侧居中
//        int currentY = 0;
//        int currentLevel = 1;
//        int countLevelOne = 0; // 重置计数器
//
//        for (Relation node : nodelList) {
//            int level = node.getLevel();
//
//            if (level > currentLevel) {
//                currentLevel = level;
//                currentX += 400;  // 重置，以便下一个节点开始
//                currentY = -200;
//                countLevelOne = 0; // 重置计数器
//            } else {
//                // 在同一层，需要移动x位置
//                currentX -= 400;  // 每次在同一层，
//            }
//
//            node.setX(currentX);
//            node.setY(currentY);
//
//            currentY += 200;
//
//            System.out.println(level);
//            System.out.println(currentLevel);
//            System.out.println(currentX);
//            System.out.println(currentY);
//            if (countLevelOne >= 2) { // 当level为1的次数大于等于2时，重置currentLevel
//                currentLevel = 1;
//            }
//        }
//        return nodelList;

        int currentX = 0;  // 设为左侧居中
        int currentY = 0;
        int currentLevel = 1;
        int countLevelOne = 0; // 重置计数器

        for (RelationTool node : nodelList) {
            int level = node.getLevel();

            if (level > currentLevel) {
                currentLevel = level;
                currentX += 400;  // 重置，以便下一个节点开始
                currentY = -200; // 重置Y坐标
                countLevelOne = 0; // 重置计数器
            } else {
                // 在同一层，需要移动x位置
                if (level % 2 == 1) { // 如果是奇数层，移动X坐标
                    currentX -= 400;  // 每次在同一层，
                }
            }

            node.setX(currentX);
            node.setY(currentY);

            currentY += 200; // 层级每增加一级，Y坐标增加200像素

            System.out.println(level);
            System.out.println(currentLevel);
            System.out.println(currentX);
            System.out.println(currentY);
            if (countLevelOne >= 2) { // 当level为1的次数大于等于2时，重置currentLevel
                currentLevel = 1;
            }
        }
        return nodelList;
    }

    private String convertNodeToXml(List<RelationTool> nodeList) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<mxGraphModel grid=\"0\">\n" +
                "    <root>\n" +
                "        <mxCell id=\"node_0\"/>\n" +
                "        <mxCell id=\"node_1\" type=\"Diagram\" group=\"ProcessControl\" parent=\"node_0\">\n" +
                "            <Object process_id=\"bill_circulaterelation\" as=\"properties\"/>\n" +
                "        </mxCell>");

        nodeList.forEach(node ->
        {
            xml.append("<mxCell id=\"" + node.getNodeId() + "\" value=\"\"\n" +
                    "                style=\"shape=billCard;whiteSpace=wrap;spacingLeft=50;spacingRight=10;overflow=hidden;resizable=0;\"\n" +
                    "                type=\"billCard\" parent=\"node_1\" vertex=\"1\" showRecords=\"false\">\n" +
                    "            <mxGeometry width=\"216.0\" height=\"132.0\" " + "x=\"" + node.getX() + "\" " + "y=\"" + node.getY() + "\" as=\"geometry\"/>\n" +
                    "            <Object as=\"properties\"\n" +
                    "                    title=\"" + node.getTitle() + "\"\n" +
                    "                    subtitle=\"" + node.getSubTitle() + "\"\n" +
                    "                    name=\"" + node.getInfo1() + "\"\n" +
                    "                    department=\"" + node.getInfo2() + "\"\n" +
                    "                    status=\"" + node.getInfo3() + "\"\n" +
                    "                    />\n" +
                    "        </mxCell>");
            if (node.getTargetNodeId() != null && !node.getTargetNodeId().isEmpty()) {
                for (int i = 0; i < node.getTargetNodeId().size(); i++) {
                    xml.append(
                            "<mxCell id=\"node_line_" + node.getNodeId() + i + "\"\n" +
                                    "                style=\"edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;jettySize=auto;orthogonalLoop=1;entryX=0;entryY=0.5;strokeColor=#A1CFFF!important;;\"\n" +
                                    "                type=\"SequenceFlow\" \n" +
                                    "                parent=\"node_1\" \n" +
                                    "                edge=\"1\" \n" +
                                    "                source=\"" + node.getNodeId() + "\"\n" +
                                    "                target=\"" + node.getTargetNodeId().get(i) + "\">\n" +
                                    "            <mxGeometry relative=\"1\" as=\"geometry\"/>\n" +
                                    "        </mxCell>");
                    xml.append("<mxCell id=\"node_line_" + node.getNodeId() + i +"_child\" parent=\"node_line_" + node.getNodeId() + i+"\" vertex=\"1\"\n" +
                            "           style=\"shape=ierp.billrelation.IconComplete;\">\n" +
                            "       <Object entityNumber=\"" + node.getTitle() + "\" businessKey=\"" + node.getSubTitle() + "\" type=\"complete\"\n" +
                            "               title=\"下推已完成\" as=\"properties\"/>\n" +
                            "       <mxGeometry width=\"24.0\" height=\"24.0\" x=\"0.7\" y=\"5.0\" relative=\"true\" as=\"geometry\">\n" +
//                            "           <mxPoint x=\""+ node.getX() +"\" y=\""+ node.getY() +"\" as=\"offset\"/>\n" +
                            "           <mxPoint x=\"-20.0\" y=\"-10.0\" as=\"offset\"/>\n" +
                            "       </mxGeometry>\n" +
                            "   </mxCell>");
                }
            }
        });
        xml.append("</root>\n" +
                "</mxGraphModel>");
        return xml.toString();
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        // 获取被点击的控件对象
        Control source = (Control) evt.getSource();
        if (StringUtils.equals(source.getKey(), "tpv_refresh")) {

        }else if (Objects.equals(source.getKey(), "tpv_goback")) {
            this.getView().close();
        }
    }
}

