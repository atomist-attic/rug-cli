# rugs

[![Build Status](https://travis-ci.org/atomist/rugs.svg?branch=master)](https://travis-ci.org/atomist/rugs)

## Testing

npm install -G typescript
npm test

## Releasing

Create a tag:

```shell
git tag m.m.m -m "Tag comment"
git push
```
The version in the package.json is replaced by the build and is totally ignored!
