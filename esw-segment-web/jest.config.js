module.exports = {
  roots: ['<rootDir>/src', '<rootDir>/test'],
  preset: 'ts-jest',
  testRegex: ['(/(test)/.*|(\\.|/)(test|spec))\\.(ts|tsx)?$'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json', 'node'],
  moduleDirectories: ['node_modules', 'src', 'test'],
  testPathIgnorePatterns: ['/test/__mocks/*'],
  verbose: true,
  moduleNameMapper: {
    '\\.(css|sass|jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
      '<rootDir>/test/__mocks__/fileMocks.ts'
  }
}
