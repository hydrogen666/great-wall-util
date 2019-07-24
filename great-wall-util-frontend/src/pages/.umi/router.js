import React from 'react';
import { Router as DefaultRouter, Route, Switch } from 'react-router-dom';
import dynamic from 'umi/dynamic';
import renderRoutes from 'umi/lib/renderRoutes';
import history from '@tmp/history';

const Router = require('dva/router').routerRedux.ConnectedRouter;

const routes = [
  {
    path: '/',
    component: require('../../layouts/index.js').default,
    routes: [
      {
        path: '/',
        exact: true,
        component: require('../index.js').default,
        _title: 'great-wall-util-frontend',
        _title_default: 'great-wall-util-frontend',
      },
      {
        component: () =>
          React.createElement(
            require('D:/project/java/great-wall-util/great-wall-util-frontend/node_modules/umi-build-dev/lib/plugins/404/NotFound.js')
              .default,
            { pagesPath: 'src/pages', hasRoutesInConfig: false },
          ),
        _title: 'great-wall-util-frontend',
        _title_default: 'great-wall-util-frontend',
      },
    ],
    _title: 'great-wall-util-frontend',
    _title_default: 'great-wall-util-frontend',
  },
  {
    component: () =>
      React.createElement(
        require('D:/project/java/great-wall-util/great-wall-util-frontend/node_modules/umi-build-dev/lib/plugins/404/NotFound.js')
          .default,
        { pagesPath: 'src/pages', hasRoutesInConfig: false },
      ),
    _title: 'great-wall-util-frontend',
    _title_default: 'great-wall-util-frontend',
  },
];
window.g_routes = routes;
const plugins = require('umi/_runtimePlugin');
plugins.applyForEach('patchRoutes', { initialValue: routes });

// route change handler
function routeChangeHandler(location, action) {
  plugins.applyForEach('onRouteChange', {
    initialValue: {
      routes,
      location,
      action,
    },
  });
}
history.listen(routeChangeHandler);
routeChangeHandler(history.location);

export { routes };

export default function RouterWrapper(props = {}) {
  return <Router history={history}>{renderRoutes(routes, props)}</Router>;
}
