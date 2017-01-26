package com.deepoove.swagger.diff.output;

import com.deepoove.swagger.diff.SwaggerDiff;

interface OutputRender {
  String render(SwaggerDiff diff);
}
