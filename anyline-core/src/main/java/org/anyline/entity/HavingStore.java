/*
 * Copyright 2006-2023 www.anyline.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.anyline.entity;

import java.util.List;

public interface HavingStore {
    List<Having> gets();
    void add(Having having) ;
    /**
     * COUNT(*) > 10
     * having("CD, NM");
     * @param having having
     */
    void add(String having) ;
    String getRunText();
    void clear();
    boolean isEmpty();
    DataRow map(boolean empty);
    List<DataRow> list(boolean empty);
    default DataRow map() {
        return map(false);
    }
    default String json(boolean empty) {
        return map(empty).json();
    }
    default String json() {
        return json(false);
    }
    HavingStore clone();
}
