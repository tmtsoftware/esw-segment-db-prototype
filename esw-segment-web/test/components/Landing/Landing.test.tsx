import React from 'react'
import { Landing } from '../../../src/components'
import { render } from '@testing-library/react'

describe('Landing', () => {
  test('should render', async () => {
    const { getByText } = render(<Landing />)
    expect(getByText('Update src/App.tsx and save to reload.')).toBeDefined()
  })
})
