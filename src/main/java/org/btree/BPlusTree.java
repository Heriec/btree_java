package org.btree;

import java.util.Deque;
import java.util.LinkedList;

public class BPlusTree<T> {

    // b+树的阶
    private Integer bTreeOrder;

    // 键的最大数量
    private Integer maxNumber;

    private Node<T> root;

    private LeafNode<T> leaf;

    public BPlusTree() {
        this(3);
    }

    // 有参构造方法，可以设定B+树的阶
    public BPlusTree(Integer bTreeOrder) {
        this.bTreeOrder = bTreeOrder;
        // 因为插入节点过程中可能出现超过上限的情况,所以这里要加1
        this.maxNumber = bTreeOrder + 1;
        this.root = new LeafNode();
        this.leaf = null;
    }

    // 查询
    public T find(Integer key) {
        T t = this.root.find(key);
        if (t == null) {
            System.out.println("不存在");
        }
        return t;
    }

    // 插入
    public void insert(T value, Integer key) {
        if (key == null)
            return;
        Node t = this.root.insert(key, value);
        if (t != null)
            this.root = t;
        // 作用是找到第一个叶子节点
        this.leaf = this.root.refreshLeft();
    }


    public void print() {
        LeafNode pointer = this.leaf;
        while (pointer != null) {
            for (int i = 0; i < pointer.number; i++) {
                if (pointer.keys[i] != null) {
                    System.out.print(pointer.keys[i] + " ");
                }
            }
            pointer = pointer.right;
        }
        System.out.println();
    }

    public void printTree() {
        Node node = this.root;
        Deque<Node> q = new LinkedList<Node>();
        q.add(node);
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                Node cur = q.poll();
                for (int j = 0; j < cur.number; j++) {
                    if (cur.keys[j] != null) {
                        System.out.print(cur.keys[j] + " ");
                        if (cur instanceof LeafNode) {

                        } else {
                            q.add(((BPlusNode) cur).children[j]);
                        }
                    }
                }
                System.out.print("      ");
            }
            System.out.println();
        }
    }


    abstract class Node<T> {
        // 父节点
        protected Node<T> parent;
        // 子节点数量
        protected Integer number;

        protected Integer[] keys;

        public Node() {
            this.keys = new Integer[maxNumber];
            this.number = 0;
            this.parent = null;
        }

        abstract T find(Integer key);


        abstract Node insert(Integer key, T value);

        abstract LeafNode refreshLeft();
    }


    class LeafNode<T> extends Node<T> {

        protected T[] values;
        protected LeafNode<T> left;
        protected LeafNode<T> right;


        public LeafNode() {
            super();
            this.values = (T[]) new Object[maxNumber];
            this.left = null;
            this.right = null;
        }

        /**
         * 经典二分查找
         */
        @SuppressWarnings("unchecked")
        @Override
        T find(Integer key) {
            if (this.number <= 0)
                return null;
            int left = 0;
            int right = this.number;

            int middle = (left + right) / 2;

            while (left < right) {
                Integer middleKey = this.keys[middle];
                if (key.compareTo(middleKey) == 0)
                    return (T) this.values[middle];
                else if (key.compareTo(middleKey) < 0)
                    right = middle;
                else
                    left = middle;
                middle = (left + right) / 2;
            }
            return null;
        }

        @Override
        Node insert(Integer key, T value) {
            // 保存原始存在父节点的key值
            Integer oldKey = null;
            if (this.number > 0)
                oldKey = this.keys[this.number - 1];
            // 先插入数据
            int i = 0;
            while (i < this.number) {
                if (key.compareTo(this.keys[i]) < 0) {
                    break;
                }
                i++;
            }

            // 复制数组,完成添加
            Integer[] tempKeys = new Integer[keys.length];
            Object[] tempValues = new Object[values.length];
            System.arraycopy(this.keys, 0, tempKeys, 0, i);
            System.arraycopy(this.values, 0, tempValues, 0, i);
            System.arraycopy(this.keys, i, tempKeys, i + 1, this.number - i);
            System.arraycopy(this.values, i, tempValues, i + 1, this.number - i);
            tempKeys[i] = key;
            tempValues[i] = value;
            this.number++;

            // 判断是否需要拆分
            // 如果不需要拆分完成复制后直接返回
            if (this.number <= bTreeOrder) {
                System.arraycopy(tempKeys, 0, this.keys, 0, this.number);
                System.arraycopy(tempValues, 0, this.values, 0, this.number);

                // 有可能虽然没有节点分裂，但是实际上插入的值大于了原来的最大值，所以所有父节点的边界值都要进行更新
                Node node = this;
                while (node.parent != null) {
                    Integer maxKey = node.keys[node.number - 1];
                    if (maxKey.compareTo(node.parent.keys[node.parent.number - 1]) > 0) {
                        node.parent.keys[node.parent.number - 1] = maxKey;
                        node = node.parent;
                    } else {
                        break;
                    }
                }
                return null;
            }

            // 如果需要拆分,则从中间把节点拆分差不多的两部分
            Integer middle = this.number / 2;
            // 新建叶子节点,作为拆分的右半部分
            LeafNode newRightLeaf = new LeafNode();
            newRightLeaf.number = this.number - middle;
            newRightLeaf.parent = this.parent;

            // 如果父节点为空,则新建一个非叶子节点作为父节点,并且让拆分成功的两个叶子节点的指针指向父节点
            if (this.parent == null) {
                BPlusNode f = new BPlusNode();
                newRightLeaf.parent = f;
                this.parent = f;
                oldKey = null;
            }

            System.arraycopy(tempKeys, middle, newRightLeaf.keys, 0, newRightLeaf.number);
            System.arraycopy(tempValues, middle, newRightLeaf.values, 0, newRightLeaf.number);

            // 让原有叶子节点作为拆分的左半部分
            this.number = middle;
            this.keys = new Integer[maxNumber];
            this.values = (T[]) new Object[maxNumber];
            System.arraycopy(tempKeys, 0, this.keys, 0, middle);
            System.arraycopy(tempValues, 0, this.values, 0, middle);

            this.right = newRightLeaf;
            newRightLeaf.left = this;

            // 叶子节点拆分成功后,需要把新生成的节点插入父节点
            BPlusNode parentNode = (BPlusNode) this.parent;
            return parentNode.insertNode(this, newRightLeaf, oldKey);

        }

        @Override
        LeafNode refreshLeft() {
            if (this.number <= 0)
                return null;
            return this;
        }
    }

    /**
     * 非叶子节点
     */
    class BPlusNode<T> extends Node<T> {
        // 子节点
        protected Node<T>[] children;

        public BPlusNode() {
            super();
            this.children = new Node[maxNumber + 1];
        }

        /**
         * 递归查找父节点范围
         */
        @Override
        T find(Integer key) {
            Integer i = 0;
            while (i < this.number) {
                if (key.compareTo(this.keys[i]) <= 0) {
                    break;
                }
                i++;
            }
            if (this.number.equals(i)) {
                return null;
            }
            return this.children[i].find(key);
        }

        @Override
        Node insert(Integer key, T value) {
            int i = 0;
            while (i < this.number) {
                if (key.compareTo(this.keys[i]) <= 0) {
                    break;
                }
                i++;
            }
            if (i >= this.number || key.compareTo(this.keys[i]) >= 0) {
                i--;
            }
            return this.children[i].insert(key, value);
        }

        @Override
        LeafNode refreshLeft() {
            return this.children[0].refreshLeft();
        }

        /**
         * 当叶子节点插入成功完成分解时,递归地向父节点插入新的节点以保持平衡
         *
         * @param leftNode
         * @param rightLeaf
         * @param parentKey
         * @return
         */
        public Node insertNode(Node leftNode, Node rightLeaf, Integer parentKey) {
            Integer oldKey = null;
            if (this.number > 0)
                oldKey = this.keys[this.number - 1];
            Integer leftMaxVal = leftNode.keys[leftNode.number - 1];
            Integer rightMaxVal = rightLeaf.keys[rightLeaf.number - 1];
            // 如果原有key为null,说明这个非节点是空的,直接放入两个节点即可
            if (oldKey == null || this.number <= 0) {
                this.keys[0] = leftMaxVal;
                this.keys[1] = rightMaxVal;
                this.children[0] = leftNode;
                this.children[1] = rightLeaf;
                this.number += 2;
                return this;
            }
            // 原有节点不为空,则应该先寻找原有节点的位置,然后将新的节点插入到原有节点中
            Integer i = 0;
            while (parentKey.compareTo(this.keys[i]) != 0) {
                i++;
            }
            // 父节点找到对应位置，左节点插入 右节点移动一格
            this.keys[i] = leftMaxVal;
            this.children[i] = leftNode;

            Object tempKeys[] = new Object[maxNumber];
            Object tempChildren[] = new Object[maxNumber];

            System.arraycopy(this.keys, 0, tempKeys, 0, i + 1);
            System.arraycopy(this.children, 0, tempChildren, 0, i + 1);
            System.arraycopy(this.keys, i + 1, tempKeys, i + 2, this.number - i - 1);
            System.arraycopy(this.children, i + 1, tempChildren, i + 2, this.number - i - 1);
            tempKeys[i + 1] = rightMaxVal;
            tempChildren[i + 1] = rightLeaf;

            this.number++;

            // 判断需不需要拆分
            if (this.number <= bTreeOrder) {

                System.arraycopy(tempKeys, 0, this.keys, 0, this.number);
                System.arraycopy(tempChildren, 0, this.children, 0, this.number);
                return null;
            }
            // 需要拆分
            Integer middle = this.number / 2;
            // 新建非叶子节点,作为拆分的右半部分
            BPlusNode tempNode = new BPlusNode();
            // 非叶节点拆分后应该将其子节点的父节点指针更新为正确的指针
            tempNode.number = this.number - middle;
            tempNode.parent = this.parent;
            // 如果父节点为空,则新建一个非叶子节点作为父节点,并且让拆分成功的两个非叶子节点的指针指向父节点
            if (this.parent == null) {
                BPlusNode tempBPlusNode = new BPlusNode();
                tempNode.parent = tempBPlusNode;
                this.parent = tempBPlusNode;
                oldKey = null;
            }
            System.arraycopy(tempKeys, middle, tempNode.keys, 0, tempNode.number);
            System.arraycopy(tempChildren, middle, tempNode.children, 0, tempNode.number);
            for (int j = 0; j < tempNode.number; j++) {
                tempNode.children[j].parent = tempNode;
            }
            // 让原有非叶子节点作为左边节点
            this.number = middle;
            this.keys = new Integer[maxNumber];
            this.children = new Node[maxNumber];
            System.arraycopy(tempKeys, 0, this.keys, 0, middle);
            System.arraycopy(tempChildren, 0, this.children, 0, middle);

            // 叶子节点拆分成功后,需要把新生成的节点插入父节点
            BPlusNode parentNode = (BPlusNode) this.parent;
            return parentNode.insertNode(this, tempNode, oldKey);
        }
    }


}
