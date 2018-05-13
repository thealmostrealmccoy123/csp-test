<!doctype html>
<%@ taglib prefix="csp" uri="/WEB-INF/taglib/csp.tld" %>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>click demo</title>
  <csp:nonce>
  <style>
  p {
    color: red;
    margin: 5px;
    cursor: pointer;
  }
  p:hover {
    background: yellow;
  }
  </style>
  <script src="https://code.jquery.com/jquery-1.10.2.js"></script>
</head>
<body>
 
<p>First Paragraph</p>
<p>Second Paragraph</p>
<p>Yet one more Paragraph</p>
 
<script>
$( "p" ).click(function() {
  $( this ).slideUp();
});
</script>
  </csp:nonce> 
</body>
</html>
