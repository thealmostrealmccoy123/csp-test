    <!doctype html>
    <%@ taglib prefix="csp" uri="/WEB-INF/taglib/csp.tld" %>
    <html ng-app="todoApp">
      <head>
       <csp:nonce>
        <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.6.9/angular.min.js"></script>
        <script src="todo.js"></script>
        <link rel="stylesheet" href="todo.css">
      </head>
      <body>
        <h2>Todo</h2>
        <div ng-controller="TodoListController as todoList">
          <span>{{todoList.remaining()}} of {{todoList.todos.length}} remaining</span>
          [ <a href="" ng-click="todoList.archive()">archive</a> ]
          <ul class="unstyled">
            <li ng-repeat="todo in todoList.todos">
              <label class="checkbox">
                <input type="checkbox" ng-model="todo.done">
                <span class="done-{{todo.done}}">{{todo.text}}</span>
              </label>
            </li>
          </ul>
          <form ng-submit="todoList.addTodo()">
            <input type="text" ng-model="todoList.todoText"  size="30"
                   placeholder="add new todo here">
            <input class="btn-primary" type="submit" value="add">
          </form>
        </div>
      </body>
      </csp:nonce>
    </html>
