/*
 * Copyright 2015-2025 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package fucksocks.server;

import java.util.List;

/**
 * The class <code>UserManager</code> represents a manager that can manage users.
 *
 * @author Youchao Feng
 * @date Apr 16, 2015 11:30:18 AM
 * @version 1.0
 *
 */
public interface UserManager {

  void addUser(String username, String password);

  User findUser(String username, String password);

  User deleteUser(String username);

  List<User> findAll();

}
