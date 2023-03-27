const defaultTheme = require('tailwindcss/defaultTheme')

module.exports = {
    // in prod look at shadow-cljs output file in dev look at runtime, which will change files that are actually compiled; postcss watch should be a whole lot faster
    // TODON: fix this
    // content: process.env.NODE_ENV == 'production' ? ["target/cljsbuild/public/js/app.js"] : ["./resources/public/js/cljs-runtime/*.js"],
    content: ["target/cljsbuild/public/js/app.js"],
    theme: {
        extend: {
            fontFamily: {
                sans: ["Inter var", ...defaultTheme.fontFamily.sans],
            },
        },
    },
    plugins: [
        require('@tailwindcss/forms'),
    ],
    prefix: 'tw-',
}
