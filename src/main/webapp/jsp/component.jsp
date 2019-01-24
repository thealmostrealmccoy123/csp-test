    <!doctype html>
<%@ taglib prefix="csp" uri="/WEB-INF/taglib/csp.tld" %>    
    <html ng-app="app">
      <head>
      <csp:nonce>
        <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.6.9/angular.min.js"></script>
        <script src="components.js"></script>
        <script src="app.js"></script>
      </head>
      <body>
        <tabs>
          <pane title="Localization">
            <span>Date: {{ '2012-04-01' | date:'fullDate' }}</span><br>
            <span>Currency: {{ 123456 | currency }}</span><br>
            <span>Number: {{ 98765.4321 | number }}</span><br>
          </pane>
          <pane title="Pluralization">
            <div ng-controller="BeerCounter">
              <div ng-repeat="beerCount in beers">
                <ng-pluralize count="beerCount" when="beerForms"></ng-pluralize>
              </div>
            </div>
          </pane>
        </tabs>
      </body>
      </csp:nonce>
    </html>
