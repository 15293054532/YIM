package com.pur.formplugin;

import java.io.Serializable;
import java.util.List;

/**
 * @author 84514
 */
public class RelationTool {

        private String nodeId;
        private String title;
        private List<String> targetNodeId;
        private String s;
        private String node1Title;
        private String node1SubTitle;
        private String info1;
        private String info2;
        private String info3;
        private Object o;
        private List<String> targetList;
        private int level;
        private Integer x;
        private Integer y;

        private String sourced;

        private String subTitle;
        private String id;



        public RelationTool(String nodeId, String title, String subTitle, String info1, String info2, String info3, String sourced, List<String> targetNodeId, int level) {
                this.nodeId = nodeId;
                this.title = title;
                this.subTitle = subTitle;
                this.info1 = info1;
                this.info2 = info2;
                this.info3 = info3;
                this.sourced = sourced;
                this.targetNodeId = targetNodeId;
                this.level = level;

        }

        public RelationTool() {

        }

        public String getNodeId() {
                return nodeId;
        }

        public void setNodeId(String nodeId) {
                this.nodeId = nodeId;
        }

        public String getTitle() {
                return title;
        }

        public void setTitle(String title) {
                this.title = title;
        }

        public List<String> getTargetNodeId() {
                return targetNodeId;
        }

        public void setTargetNodeId(List<String> targetNodeId) {
                this.targetNodeId = targetNodeId;
        }

        public String getS() {
                return s;
        }

        public void setS(String s) {
                this.s = s;
        }

        public String getNode1Title() {
                return node1Title;
        }

        public void setNode1Title(String node1Title) {
                this.node1Title = node1Title;
        }

        public String getNode1SubTitle() {
                return node1SubTitle;
        }

        public void setNode1SubTitle(String node1SubTitle) {
                this.node1SubTitle = node1SubTitle;
        }

        public String getInfo1() {
                return info1;
        }

        public void setInfo1(String info1) {
                this.info1 = info1;
        }

        public String getInfo2() {
                return info2;
        }

        public void setInfo2(String info2) {
                this.info2 = info2;
        }

        public String getInfo3() {
                return info3;
        }

        public void setInfo3(String info3) {
                this.info3 = info3;
        }

        public Object getO() {
                return o;
        }

        public void setO(Object o) {
                this.o = o;
        }

        public List<String> getTargetList() {
                return targetList;
        }

        public void setTargetList(List<String> targetList) {
                this.targetList = targetList;
        }

        public int getLevel() {
                return level;
        }

        public void setLevel(int i) {
                this.level = i;
        }

        public Integer getX() {
                return x;
        }

        public void setX(Integer x) {
                this.x = x;
        }

        public Integer getY() {
                return y;
        }

        public void setY(Integer y) {
                this.y = y;
        }

        public String getSubTitle() {
                return subTitle;
        }

        public String getSourced() {
                return sourced;
        }

        public void setSourced(String sourced) {
                this.sourced = sourced;
        }

        public void setSubTitle(String subTitle) {
                this.subTitle = subTitle;
        }


        public void setSourceNodeId(String id) {
                this.id=id;
        }

        public String getId() {
                return id;
        }

        public void setId(String id) {
                this.id = id;
        }

        private boolean virtual;
        private List<RelationTool> targets;
        private int width;
        public void setVirtual(boolean virtual) {
                this.virtual = virtual;
        }

        public void setTargets(List<RelationTool> targets) {
                this.targets = targets;
        }

        public List<RelationTool> getTargets() {
            return targets;
        }

        public int getWidth() {
                return width;
        }

}
