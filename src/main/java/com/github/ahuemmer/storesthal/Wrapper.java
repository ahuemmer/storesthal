package com.github.ahuemmer.storesthal;

import java.util.List;

public class Wrapper<T> {
        private List<T> data;

        public Wrapper() {}

        public Wrapper(List<T> data) {
            this.data = data;
        }
        public List<T> getData() {
            return data;
        }
        public void setData(List<T> data) {
            this.data = data;
        }
    }
